package cv.igrp.framework.auth.core.security;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extracts roles, permissions and departments straight from the caller's JWT
 * instead of round-tripping back to the Access Management API. The parsed
 * result is cached per-JWT so repeated {@code @PreAuthorize} evaluations
 * across a request share one parse; the cache invalidates naturally when the
 * token rotates because the JWT string itself is the cache key.
 *
 * <h2>JWT claims consumed</h2>
 * <ul>
 *   <li>{@code permissions} — flat {@code Set<String>} of permission names (e.g.
 *       {@code "parametrizacao.agencia.listar"}). Populated by the AM's
 *       {@code ClaimsEnrichmentService} on token issuance.</li>
 *   <li>{@code selectedRole} — the caller's currently-active role code. Used as
 *       the single element of {@link #getRoles(String)}'s return set. The full
 *       role set is available on the JWT under
 *       {@code resource_access.<clientId>.roles} but is intentionally not read
 *       for authorization (see the note on {@link #getRoles(String)}).</li>
 *   <li>{@code org} — the active role's department code. Used as the single
 *       element of {@link #getDepartments(String)}'s return set.</li>
 *   <li>{@code is_super_admin} — boolean; drives {@link #isSuperAdmin(String)}.
 *       Populated by AM's {@code ClaimsEnrichmentService.isSuperAdmin(user)}.</li>
 * </ul>
 *
 * <h2>Semantic change vs the previous API-backed adapter</h2>
 * Both {@link #getRoles(String)} and {@link #getDepartments(String)} now
 * return a <strong>single-element set describing the currently-active role</strong>
 * (the role code and its department, respectively). The previous impl returned
 * the union of every role / department the user held — meaning a user with
 * roles X and Y active on Y still satisfied
 * {@code @PreAuthorize("hasAnyRole('X')")}. That silently bypassed the
 * "act as one role at a time" model iGRP is built around. Consumers that need
 * the full role or department set for UI purposes (a "switch active role"
 * picker, for example) must go through the AM client directly — authorization
 * is intentionally scoped to the active role only.
 *
 * <h2>Prerequisites in the consumer app</h2>
 * Caching only takes effect when Spring's cache infrastructure is enabled.
 * Add {@code @EnableCaching} to a {@code @Configuration} class (or set
 * {@code spring.cache.type=simple} + rely on Spring Boot's auto-configuration).
 * Without it the {@code @Cacheable} annotations degrade to plain method calls —
 * still correct, just no caching.
 *
 * <h2>Failure behaviour</h2>
 * Every method logs and returns an empty set on parse failure so a malformed
 * token never surfaces as an unauthenticated null-pointer in downstream
 * authorization logic. A missing claim (as opposed to a malformed token) also
 * returns an empty set.
 */
@Component
@ConditionalOnProperty(
        name = "igrp.authorization.service.adapter",
        havingValue = "igrp",
        matchIfMissing = true
)
public class IgrpAuthorizationServiceAdapter implements IAuthorizationServiceAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(IgrpAuthorizationServiceAdapter.class);

    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_SELECTED_ROLE = "selectedRole";
    private static final String CLAIM_ORG = "org";
    private static final String CLAIM_IS_SUPER_ADMIN = "is_super_admin";

    @Override
    @Cacheable(value = "igrp.auth.rolesCache", key = "#p0", unless = "#result.isEmpty()")
    public Set<String> getRoles(String jwt) {
        try {
            Map<String, Object> claims = parseClaims(jwt);
            // Return ONLY the caller's currently-active role, not the union of
            // every role they hold. Mirrors the semantic used by getDepartments
            // (which returns only the active role's department via the `org`
            // claim). If both were flattened, a user with roles X and Y and
            // active role Y would still satisfy @PreAuthorize("hasAnyRole('X')")
            // — silently bypassing the "act as one role at a time" model.
            //
            // The full role set is still present on the JWT under
            // `resource_access.<clientId>.roles` for consumers that need to
            // display "roles available to switch to"; authorization does not
            // read it.
            Object selectedRole = claims.get(CLAIM_SELECTED_ROLE);
            if (selectedRole instanceof String s && !s.isBlank()) {
                return Collections.singleton(s);
            }
            LOGGER.debug("JWT has no '{}' claim; returning empty role set", CLAIM_SELECTED_ROLE);
            return Set.of();
        } catch (Exception e) {
            LOGGER.error("Error extracting active role from JWT", e);
            return Set.of();
        }
    }

    @Override
    @Cacheable(value = "igrp.auth.permissionsCache", key = "#p0", unless = "#result.isEmpty()")
    public Set<String> getPermissions(String jwt) {
        try {
            Map<String, Object> claims = parseClaims(jwt);
            Object permissions = claims.get(CLAIM_PERMISSIONS);
            if (!(permissions instanceof Collection<?> col)) {
                LOGGER.debug("JWT has no '{}' claim; returning empty permission set", CLAIM_PERMISSIONS);
                return Set.of();
            }
            Set<String> result = col.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());
            LOGGER.debug("Extracted {} permission(s) from JWT", result.size());
            return result;
        } catch (Exception e) {
            LOGGER.error("Error extracting permissions from JWT", e);
            return Set.of();
        }
    }

    @Override
    @Cacheable(value = "igrp.auth.departmentsCache", key = "#p0", unless = "#result.isEmpty()")
    public Set<String> getDepartments(String jwt) {
        try {
            Map<String, Object> claims = parseClaims(jwt);
            Object org = claims.get(CLAIM_ORG);
            if (org instanceof String s && !s.isBlank()) {
                return Collections.singleton(s);
            }
            LOGGER.debug("JWT has no '{}' claim; returning empty department set", CLAIM_ORG);
            return Set.of();
        } catch (Exception e) {
            LOGGER.error("Error extracting departments from JWT", e);
            return Set.of();
        }
    }

    @Override
    @Cacheable(value = "igrp.auth.superAdminCache", key = "#p0")
    public boolean isSuperAdmin(String jwt) {
        try {
            Map<String, Object> claims = parseClaims(jwt);
            Object claim = claims.get(CLAIM_IS_SUPER_ADMIN);
            if (claim instanceof Boolean b) {
                return b;
            }
            // Defensive: some IdPs / test rigs stamp boolean claims as strings.
            if (claim instanceof String s) {
                return Boolean.parseBoolean(s);
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("Error extracting is_super_admin from JWT", e);
            return false;
        }
    }

    /**
     * Parse the JWT payload without re-verifying the signature — Spring
     * Security's resource-server filter already verified it before this
     * adapter is called (that is why the JWT string reached us at all).
     */
    private Map<String, Object> parseClaims(String jwt) throws java.text.ParseException {
        JWT parsed = JWTParser.parse(jwt);
        return parsed.getJWTClaimsSet().getClaims();
    }
}
