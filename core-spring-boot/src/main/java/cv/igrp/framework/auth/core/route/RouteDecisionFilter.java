package cv.igrp.framework.auth.core.route;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * The route decision (v3-07), after Spring Security has validated the token and answered
 * 401 to anonymous callers on every non-public request:
 * <ol>
 *   <li>public routes, static resources and paths with no handler pass (the last get the normal 404);</li>
 *   <li>root ({@code is_super_admin}) is allowed without a lookup, and audited;</li>
 *   <li>any other handler that is not a registered route (actuator, Spring's own) is refused;</li>
 *   <li>a route is allowed when the caller's permission set has its key, otherwise 403 and an audited denial.</li>
 * </ol>
 * AM no longer authorizing the caller is a 401; neither Redis nor AM answering is a 503.
 */
public class RouteDecisionFilter extends OncePerRequestFilter {

    static final String ROOT_CLAIM = "is_super_admin";

    private final Supplier<RouteResolver> resolver;
    private final PermissionLookup lookup;
    private final AccessAudit audit;

    /** @param resolver the shared resolver, built once the handler mappings exist */
    public RouteDecisionFilter(Supplier<RouteResolver> resolver, PermissionLookup lookup, AccessAudit audit) {
        this.resolver = resolver;
        this.lookup = lookup;
        this.audit = audit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RouteResolver.Resolution resolution = resolver.get().resolve(request);
        if (resolution.kind() != RouteResolver.Kind.ROUTE && resolution.kind() != RouteResolver.Kind.OTHER) {
            chain.doFilter(request, response);
            return;
        }
        Jwt jwt = callerToken();
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (isRoot(jwt)) {
            audit.root(method, path, resolution.key());
            chain.doFilter(request, response);
            return;
        }
        if (resolution.kind() == RouteResolver.Kind.OTHER) {
            audit.denied(method, path, null);
            throw new AccessDeniedException("Not a route of this service");
        }
        switch (lookup.check(jwt.getSubject(), jwt.getTokenValue(), resolution.key())) {
            case ALLOWED -> chain.doFilter(request, response);
            case DENIED -> {
                audit.denied(method, path, resolution.key());
                throw new AccessDeniedException("Missing permission " + resolution.key());
            }
            case UNAUTHENTICATED -> throw new InsufficientAuthenticationException("AM no longer authorizes the caller");
            case UNAVAILABLE -> response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }

    /** Spring Security's final rule already refused anonymous callers; anything else than a JWT has no permission set. */
    private static Jwt callerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token && token.isAuthenticated()) {
            return token.getToken();
        }
        throw new AccessDeniedException("Only bearer tokens carry a permission set");
    }

    private static boolean isRoot(Jwt jwt) {
        Object claim = jwt.getClaims().get(ROOT_CLAIM);
        return Boolean.TRUE.equals(claim) || "true".equals(claim);
    }
}
