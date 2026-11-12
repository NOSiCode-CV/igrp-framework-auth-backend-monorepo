package cv.igrp.framework.auth.core.route;

import cv.igrp.framework.auth.core.scope.IgrpScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Whether a user holds a permission key in this service's application (v3-07). Reads the
 * set AM writes in Redis; on a miss, or while Redis is unavailable, asks AM with the
 * caller's own token. The starter never writes the set: only AM does.
 *
 * <p>Redis contract (#14): the Set {@code permissions:user:{sub}:app:{appCode}} holds the
 * keys plus the marker {@value #MARKER}, so a written set always exists. The set exists:
 * a hit, even with no keys. It does not exist: a miss. Next to it, written with it (v3-14):
 * the Hash {@code ...:scopes} (key to scope), and the Sets {@code ...:roles} (the caller's
 * role codes) and {@code ...:departments} (its visible department subtree).
 */
public class PermissionLookup {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionLookup.class);

    static final String MARKER = "~";

    /** What the lookup found out. */
    public enum Answer {
        ALLOWED,
        DENIED,
        /** AM no longer authorizes the caller (inactive user, revoked client): 401. */
        UNAUTHENTICATED,
        /** Neither Redis nor AM could answer: the request is refused with 503, never allowed. */
        UNAVAILABLE
    }

    /** The body of {@code GET /api/users/me/permissions}; roles and departments feed the data scopes. */
    record PermissionSet(List<String> keys, Map<String, String> scopes, List<String> roles, List<String> departments) {
    }

    /**
     * The decision on one key and, when allowed, the data scope that comes with it (v3-14);
     * {@code scope} is {@code null} when the key's scope is missing or has an unknown code.
     */
    public record Access(Answer answer, IgrpScope.Context scope) {
    }

    private final StringRedisTemplate redis;
    private final RestClient am;
    private final String applicationCode;

    public PermissionLookup(StringRedisTemplate redis, RestClient am, String applicationCode) {
        this.redis = redis;
        this.am = am;
        this.applicationCode = applicationCode;
    }

    public Answer check(String sub, String token, String key) {
        return access(sub, token, key).answer();
    }

    /** Whether the caller holds the key, and with which data scope. */
    public Access access(String sub, String token, String key) {
        return fromRedis(sub, key).orElseGet(() -> {
            Fetch fetch = fetch(token);
            if (fetch.answer() != Answer.ALLOWED) {
                return new Access(fetch.answer(), null);
            }
            if (!fetch.keys().contains(key)) {
                return new Access(Answer.DENIED, null);
            }
            return new Access(Answer.ALLOWED,
                    IgrpScope.from(fetch.scopes().get(key), sub, fetch.roles(), fetch.departments()));
        });
    }

    /**
     * The caller's whole permission set (v3-08), for method security on code with no route.
     * Empty when neither Redis nor AM can give it: a check against it then denies.
     */
    public Set<String> permissions(String sub, String token) {
        return setFromRedis(sub).orElseGet(() -> Set.copyOf(fetch(token).keys()));
    }

    private Optional<Set<String>> setFromRedis(String sub) {
        if (redis == null) {
            return Optional.empty();
        }
        try {
            Set<String> members = redis.opsForSet().members(setKey(sub));
            if (!members.contains(MARKER)) {
                return Optional.empty();
            }
            Set<String> keys = new HashSet<>(members);
            keys.remove(MARKER);
            return Optional.of(Set.copyOf(keys));
        } catch (RuntimeException ex) {
            LOGGER.warn("[Route Filter] Redis unavailable, asking AM for the permissions of '{}': {}", sub, ex.getMessage());
            return Optional.empty();
        }
    }

    private String setKey(String sub) {
        return "permissions:user:" + sub + ":app:" + applicationCode;
    }

    /** @return the answer from the set; empty on a miss or when Redis cannot be read */
    private Optional<Access> fromRedis(String sub, String key) {
        if (redis == null) {
            return Optional.empty();
        }
        try {
            Map<Object, Boolean> members = redis.opsForSet().isMember(setKey(sub), MARKER, key);
            if (members == null || !Boolean.TRUE.equals(members.get(MARKER))) {
                return Optional.empty();
            }
            if (!Boolean.TRUE.equals(members.get(key))) {
                return Optional.of(new Access(Answer.DENIED, null));
            }
            String set = setKey(sub);
            Object scope = redis.opsForHash().get(set + ":scopes", key);
            return Optional.of(new Access(Answer.ALLOWED, IgrpScope.from(scope == null ? null : scope.toString(), sub,
                    redis.opsForSet().members(set + ":roles"), redis.opsForSet().members(set + ":departments"))));
        } catch (RuntimeException ex) {
            LOGGER.warn("[Route Filter] Redis unavailable, asking AM for the permissions of '{}': {}", sub, ex.getMessage());
            return Optional.empty();
        }
    }

    /** AM's answer to the permission fetch: ALLOWED with the set, or why there is none. */
    private record Fetch(Answer answer, List<String> keys, Map<String, String> scopes, List<String> roles,
                         List<String> departments) {

        static Fetch refused(Answer answer) {
            return new Fetch(answer, List.of(), Map.of(), List.of(), List.of());
        }
    }

    private Fetch fetch(String token) {
        try {
            PermissionSet set = am.get()
                    .uri(uri -> uri.path("/api/users/me/permissions").queryParam("applicationCode", applicationCode).build())
                    .header("Authorization", "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PermissionSet.class);
            if (set == null) {
                return new Fetch(Answer.ALLOWED, List.of(), Map.of(), List.of(), List.of());
            }
            return new Fetch(Answer.ALLOWED, orEmpty(set.keys()), set.scopes() == null ? Map.of() : set.scopes(),
                    orEmpty(set.roles()), orEmpty(set.departments()));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                return Fetch.refused(Answer.UNAUTHENTICATED);
            }
            LOGGER.error("[Route Filter] AM answered HTTP {} to the permission fetch of application '{}'",
                    ex.getStatusCode().value(), applicationCode);
            return Fetch.refused(Answer.UNAVAILABLE);
        } catch (RestClientException ex) {
            LOGGER.error("[Route Filter] AM unreachable for the permission fetch of application '{}'", applicationCode, ex);
            return Fetch.refused(Answer.UNAVAILABLE);
        }
    }

    private static List<String> orEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }
}
