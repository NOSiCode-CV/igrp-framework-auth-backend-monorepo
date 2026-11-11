package cv.igrp.framework.auth.core.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Whether a user holds a permission key in this service's application (v3-07). Reads the
 * set AM writes in Redis; on a miss, or while Redis is unavailable, asks AM with the
 * caller's own token. The starter never writes the set: only AM does.
 *
 * <p>Redis contract (#14): the Set {@code permissions:user:{sub}:app:{appCode}} holds the
 * keys plus the marker {@value #MARKER}, so a written set always exists. The set exists:
 * a hit, even with no keys. It does not exist: a miss.
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

    /** The body of {@code GET /api/users/me/permissions}; scopes are read by the scope filter. */
    record PermissionSet(List<String> keys, Map<String, String> scopes) {
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
        Boolean cached = fromRedis(sub, key);
        if (cached != null) {
            return cached ? Answer.ALLOWED : Answer.DENIED;
        }
        return fromAm(token, key);
    }

    /** @return the answer from the set, or {@code null} on a miss or when Redis cannot be read */
    private Boolean fromRedis(String sub, String key) {
        if (redis == null) {
            return null;
        }
        try {
            Map<Object, Boolean> members = redis.opsForSet()
                    .isMember("permissions:user:" + sub + ":app:" + applicationCode, MARKER, key);
            if (members == null || !Boolean.TRUE.equals(members.get(MARKER))) {
                return null;
            }
            return Boolean.TRUE.equals(members.get(key));
        } catch (RuntimeException ex) {
            LOGGER.warn("[Route Filter] Redis unavailable, asking AM for the permissions of '{}': {}", sub, ex.getMessage());
            return null;
        }
    }

    private Answer fromAm(String token, String key) {
        try {
            PermissionSet set = am.get()
                    .uri(uri -> uri.path("/api/users/me/permissions").queryParam("applicationCode", applicationCode).build())
                    .header("Authorization", "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PermissionSet.class);
            List<String> keys = set == null || set.keys() == null ? List.of() : set.keys();
            return keys.contains(key) ? Answer.ALLOWED : Answer.DENIED;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                return Answer.UNAUTHENTICATED;
            }
            LOGGER.error("[Route Filter] AM answered HTTP {} to the permission fetch of application '{}'",
                    ex.getStatusCode().value(), applicationCode);
            return Answer.UNAVAILABLE;
        } catch (RestClientException ex) {
            LOGGER.error("[Route Filter] AM unreachable for the permission fetch of application '{}'", applicationCode, ex);
            return Answer.UNAVAILABLE;
        }
    }
}
