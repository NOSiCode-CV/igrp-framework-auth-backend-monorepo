package cv.igrp.framework.auth.core.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Root ({@code is_super_admin} in the token) is allowed everything without a permission
 * check: every route, every field. Its requests are audited by the route filter.
 */
public final class RootAccess {

    public static final String CLAIM = "is_super_admin";

    private RootAccess() {
    }

    public static boolean isRoot(Jwt jwt) {
        Object claim = jwt.getClaims().get(CLAIM);
        return Boolean.TRUE.equals(claim) || "true".equals(claim);
    }
}
