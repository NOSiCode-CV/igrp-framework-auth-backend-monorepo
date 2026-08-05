package cv.igrp.framework.auth.core.security;

import cv.igrp.framework.auth.generated.PermissionsRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service("igrpAuthorization")
@SuppressWarnings("unused")
public class IgrpAuthorizationService {

    /**
     * Marker authority stamped by {@link IgrpJwtAuthenticationConverter} when
     * {@code IAuthorizationServiceAdapter.isSuperAdmin(jwt)} returns true.
     * Every {@code check*} method in this service short-circuits to {@code true}
     * when the current caller has this authority — a super-admin passes every
     * framework-driven permission check without needing the specific permission
     * granted.
     *
     * <p>For consumers writing {@code @PreAuthorize("hasAnyRole('X')")} against
     * Spring's own SpEL (which does not consult this service), compose the
     * super-admin bypass explicitly:
     * <pre>{@code
     * @PreAuthorize("@igrpAuthorization.isSuperAdmin() or hasAnyRole('X')")
     * }</pre>
     */
    public static final String SUPER_ADMIN_AUTHORITY = "IGRP_SUPER_ADMIN";

    /**
     * Extracts all granted authorities for the current authenticated user.
     */
    private Set<String> getCurrentAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getAuthorities() == null) {
            return Set.of();
        }

        return auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /**
     * True when the current caller is a super-admin. Wired at JWT-conversion
     * time from the {@code is_super_admin} claim; readable from any SpEL
     * expression via {@code @igrpAuthorization.isSuperAdmin()}.
     */
    public boolean isSuperAdmin() {
        return getCurrentAuthorities().contains(SUPER_ADMIN_AUTHORITY);
    }

    /**
     * Checks if the current user has a specific permission.
     * Super-admins pass unconditionally.
     */
    public boolean checkPermission(PermissionsRegistry.Permission action) {
        if (action == null) return false;

        Set<String> authorities = getCurrentAuthorities();
        if (authorities.contains(SUPER_ADMIN_AUTHORITY)) return true;

        return authorities.contains(action.getCode());
    }

    /**
     * Checks if the user has ALL provided permissions.
     * Super-admins pass unconditionally.
     */
    public boolean checkAllPermissions(PermissionsRegistry.Permission... actions) {
        if (actions == null || actions.length == 0) {
            return false;
        }

        Set<String> authorities = getCurrentAuthorities();
        if (authorities.contains(SUPER_ADMIN_AUTHORITY)) return true;

        return Arrays.stream(actions)
                .map(PermissionsRegistry.Permission::getCode)
                .allMatch(authorities::contains);
    }

    /**
     * Checks if the user has AT LEAST ONE of the provided permissions.
     * Super-admins pass unconditionally.
     */
    public boolean checkAnyPermission(PermissionsRegistry.Permission... actions) {
        if (actions == null || actions.length == 0) {
            return false;
        }

        Set<String> authorities = getCurrentAuthorities();
        if (authorities.contains(SUPER_ADMIN_AUTHORITY)) return true;

        return Arrays.stream(actions)
                .map(PermissionsRegistry.Permission::getCode)
                .anyMatch(authorities::contains);
    }
}
