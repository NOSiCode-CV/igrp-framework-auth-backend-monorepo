package cv.igrp.framework.auth.core.security;

import cv.igrp.framework.auth.core.route.PermissionLookup;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.function.SingletonSupplier;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Turns a validated JWT into the caller's authentication (v3-08). Its authorities are the
 * caller's permission keys in this service's application, read from the set AM writes in
 * Redis, or from AM on a miss. They are loaded only when something asks for them (a
 * {@code @PreAuthorize} on code with no route), once per request. Permissions and roles
 * are no longer token claims.
 */
public class PermissionAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /** The factor Spring Security 7's own JWT converter gives every bearer token. */
    static final String BEARER_FACTOR = "FACTOR_BEARER";

    private final PermissionLookup lookup;

    public PermissionAuthenticationConverter(PermissionLookup lookup) {
        this.lookup = lookup;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new PermissionAuthentication(jwt, () -> lookup.permissions(jwt.getSubject(), jwt.getTokenValue()).stream()
                .map(key -> (GrantedAuthority) new SimpleGrantedAuthority(key))
                .toList());
    }

    /** A JWT authentication whose authorities are the permission keys, loaded on first use. */
    public static final class PermissionAuthentication extends JwtAuthenticationToken {

        @Serial
        private static final long serialVersionUID = 1L;

        private final transient Supplier<List<GrantedAuthority>> permissions;

        PermissionAuthentication(Jwt jwt, Supplier<List<GrantedAuthority>> permissions) {
            // Authenticated by a bearer token, as Spring Security's own converter marks it;
            // the permission keys come from the supplier.
            super(jwt, List.of(FactorGrantedAuthority.fromAuthority(BEARER_FACTOR)), jwt.getSubject());
            this.permissions = SingletonSupplier.of(permissions);
        }

        @Override
        public Collection<GrantedAuthority> getAuthorities() {
            List<GrantedAuthority> all = new ArrayList<>(super.getAuthorities());
            if (permissions != null) {
                all.addAll(permissions.get());
            }
            return Collections.unmodifiableList(all);
        }

        @Override
        public boolean equals(Object other) {
            return super.equals(other);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }
}
