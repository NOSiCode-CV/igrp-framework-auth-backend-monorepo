package cv.igrp.framework.auth.core.route;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.PathContainer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds what a request will reach, before the dispatcher runs, so the route filter can
 * decide on it (v3-07). The result is kept on the request: Spring Security's public
 * matcher and the route filter ask for it once each.
 */
public final class RouteResolver {

    /** What a request reaches. {@code key} is set only for {@link Kind#ROUTE}. */
    public record Resolution(Kind kind, String key) {

        static final Resolution NO_HANDLER = new Resolution(Kind.NO_HANDLER, null);
        static final Resolution PUBLIC = new Resolution(Kind.PUBLIC, null);
        static final Resolution STATIC = new Resolution(Kind.STATIC, null);
        static final Resolution OTHER = new Resolution(Kind.OTHER, null);
    }

    public enum Kind {
        /** A registered route of the service: decided by its permission key. */
        ROUTE,
        /** {@link Public} on the handler, or a match in {@code igrp.security.public-paths}. */
        PUBLIC,
        /** A static resource: served or 404, never a route. */
        STATIC,
        /** No handler at all: the dispatcher answers 404. */
        NO_HANDLER,
        /** Any other handler (actuator, Spring's own controllers, method-less mappings): closed. */
        OTHER
    }

    private static final String ATTRIBUTE = RouteResolver.class.getName() + ".resolution";

    private final String service;
    private final List<HandlerMapping> mappings;
    private final List<PathPattern> publicPaths;
    private final Map<Method, RequestMappingInfo> routes = new HashMap<>();

    /**
     * @param mappings    the dispatcher's handler mappings, in their order
     * @param publicPaths path patterns that are public in this environment
     */
    public RouteResolver(String service, List<HandlerMapping> mappings, List<String> publicPaths) {
        this.service = service;
        this.mappings = List.copyOf(mappings);
        this.publicPaths = publicPaths.stream().filter(path -> !path.isBlank())
                .map(path -> PathPatternParser.defaultInstance.parse(path.trim())).toList();
        for (HandlerMapping mapping : this.mappings) {
            if (mapping instanceof RequestMappingHandlerMapping requestMappings) {
                requestMappings.getHandlerMethods().forEach((info, handler) -> {
                    if (RouteCatalog.isRegistered(info, handler)) {
                        routes.put(handler.getMethod(), info);
                    }
                });
            }
        }
    }

    public Resolution resolve(HttpServletRequest request) {
        if (request.getAttribute(ATTRIBUTE) instanceof Resolution cached) {
            return cached;
        }
        Resolution resolution = compute(request);
        request.setAttribute(ATTRIBUTE, resolution);
        return resolution;
    }

    private Resolution compute(HttpServletRequest request) {
        PathContainer path = PathContainer.parsePath(request.getRequestURI().substring(request.getContextPath().length()));
        if (publicPaths.stream().anyMatch(pattern -> pattern.matches(path))) {
            return Resolution.PUBLIC;
        }
        Object handler = handlerFor(request);
        if (handler == null) {
            return Resolution.NO_HANDLER;
        }
        if (handler instanceof HandlerMethod method) {
            RequestMappingInfo info = routes.get(method.getMethod());
            if (info != null) {
                return new Resolution(Kind.ROUTE, RouteCatalog.keyFor(service, method,
                        declaredMethod(info, request.getMethod()), patternOf(request)));
            }
            return RouteCatalog.isPublic(method) ? Resolution.PUBLIC : Resolution.OTHER;
        }
        return handler instanceof ResourceHttpRequestHandler ? Resolution.STATIC : Resolution.OTHER;
    }

    /** The first handler mapping that knows the request, as the dispatcher would pick it. */
    private Object handlerFor(HttpServletRequest request) {
        boolean parsedHere = !ServletRequestPathUtils.hasParsedRequestPath(request);
        if (parsedHere) {
            ServletRequestPathUtils.parseAndCache(request);
        }
        try {
            for (HandlerMapping mapping : mappings) {
                HandlerExecutionChain chain = mapping.getHandler(request);
                if (chain != null) {
                    return chain.getHandler();
                }
            }
            return null;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not resolve the handler of " + request.getMethod() + " "
                    + request.getRequestURI(), ex);
        } finally {
            if (parsedHere) {
                ServletRequestPathUtils.clearParsedRequestPath(request);
            }
        }
    }

    /** A HEAD on a GET mapping is served by the GET handler, so it needs the GET key. */
    private static String declaredMethod(RequestMappingInfo info, String requestMethod) {
        boolean declared = info.getMethodsCondition().getMethods().stream()
                .anyMatch(method -> method.name().equals(requestMethod));
        return !declared && "HEAD".equals(requestMethod) ? "GET" : requestMethod;
    }

    private static String patternOf(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? request.getRequestURI() : pattern.toString();
    }
}
