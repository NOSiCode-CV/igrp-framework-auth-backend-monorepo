package cv.igrp.framework.auth.core.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.HashSet;
import java.util.Set;


@Configuration
public class IgrpJwtAuthenticationConverter {

    @Bean
    @ConditionalOnBean(IAuthorizationServiceAdapter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter(IAuthorizationServiceAdapter authorizationService) {

        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            Set<GrantedAuthority> authorities = new HashSet<>();

            final String token = jwt.getTokenValue();
            authorizationService
                    .getRoles(token)
                    .forEach(r -> {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                        authorities.add(new SimpleGrantedAuthority("GROUP_" + r));
                    });

            authorizationService
                    .getPermissions(token)
                    .forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));

            authorizationService
                    .getDepartments(token)
                    .forEach(d -> {
                        authorities.add(new SimpleGrantedAuthority(d));
                        authorities.add(new SimpleGrantedAuthority("GROUP_" + d));
                    });

            // Stamp the super-admin marker authority when the JWT says so.
            // IgrpAuthorizationService.check{Permission,AllPermissions,AnyPermission}
            // short-circuits to true when this authority is present, so super-admins
            // pass every framework-driven permission check without having to be
            // granted each permission individually. For hasAnyRole('X') / hasRole('X')
            // checks (Spring's own SpEL), compose the marker via
            // @PreAuthorize("@igrpAuthorization.isSuperAdmin() or hasAnyRole('X')").
            if (authorizationService.isSuperAdmin(token)) {
                authorities.add(new SimpleGrantedAuthority(IgrpAuthorizationService.SUPER_ADMIN_AUTHORITY));
            }

            // Always include activiti-user role
            authorities.add(new SimpleGrantedAuthority("ROLE_ACTIVITI_USER"));

            return authorities;

        });

        return converter;
    }

}
