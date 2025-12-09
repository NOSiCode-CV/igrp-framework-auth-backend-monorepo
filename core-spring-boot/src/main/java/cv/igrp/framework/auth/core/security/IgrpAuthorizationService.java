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
     * Checks if the current user has a specific permission.
     * Permissions come from JWT converter as simple string authorities.
     */
    public boolean checkPermission(PermissionsRegistry.Permission action) {
        if (action == null) return false;

        String permissionCode = action.getCode();
        Set<String> authorities = getCurrentAuthorities();

        return authorities.contains(permissionCode);
    }

    /**
     * Checks if the user has ALL provided permissions.
     */
    public boolean checkAllPermissions(PermissionsRegistry.Permission... actions) {
        if (actions == null || actions.length == 0) {
            return false;
        }

        Set<String> authorities = getCurrentAuthorities();

        return Arrays.stream(actions)
                .map(PermissionsRegistry.Permission::getCode)
                .allMatch(authorities::contains);
    }

    /**
     * Checks if the user has AT LEAST ONE of the provided permissions.
     */
    public boolean checkAnyPermission(PermissionsRegistry.Permission... actions) {
        if (actions == null || actions.length == 0) {
            return false;
        }

        Set<String> authorities = getCurrentAuthorities();

        return Arrays.stream(actions)
                .map(PermissionsRegistry.Permission::getCode)
                .anyMatch(authorities::contains);
    }
}
