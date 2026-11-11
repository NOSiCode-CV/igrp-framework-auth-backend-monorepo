package cv.igrp.framework.auth.core.route;

import jakarta.servlet.http.HttpServletRequest;
import cv.igrp.framework.auth.core.security.PermissionAuthenticationConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.function.SingletonSupplier;

import java.util.function.Supplier;

/**
 * The starter's route security (v3-07), applied to a {@link HttpSecurity}: public routes
 * are {@code permitAll}, everything else {@code authenticated} (so anonymous callers get
 * 401 before any lookup), and the {@link RouteDecisionFilter} makes the 403 decision.
 *
 * <p>The starter applies it to its own default chain. A service that defines its own
 * {@code SecurityFilterChain} applies it with one line, and must not add its own
 * {@code authorizeHttpRequests}. It also sets up the resource server, whose JWT
 * authorities are the caller's permission keys:
 * <pre>{@code
 * @Bean
 * SecurityFilterChain api(HttpSecurity http, RouteSecurity routeSecurity) {
 *     routeSecurity.apply(http);
 *     return http.build();
 * }
 * }</pre>
 * Without it, the service does not start.
 */
public class RouteSecurity {

    private final Supplier<RouteResolver> resolver;
    private final PermissionLookup lookup;
    private final PermissionAuthenticationConverter converter;
    private final AccessAudit audit;

    /** @param resolver built on first use, once the handler mappings exist */
    public RouteSecurity(Supplier<RouteResolver> resolver, PermissionLookup lookup,
                         PermissionAuthenticationConverter converter, AccessAudit audit) {
        this.resolver = SingletonSupplier.of(resolver);
        this.lookup = lookup;
        this.converter = converter;
        this.audit = audit;
    }

    public HttpSecurity apply(HttpSecurity http) {
        RequestMatcher publicRoutes = this::isPublic;
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers(publicRoutes).permitAll()
                .anyRequest().authenticated());
        http.addFilterAfter(new RouteDecisionFilter(resolver, lookup, audit), AuthorizationFilter.class);
        // The bearer token's authorities are the caller's permission keys (v3-08).
        http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        return http;
    }

    private boolean isPublic(HttpServletRequest request) {
        return resolver.get().resolve(request).kind() == RouteResolver.Kind.PUBLIC;
    }
}
