package cv.igrp.framework.auth.core.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Turns a service's handler methods into its routes, one per HTTP method and path,
 * each with its permission key, and enforces the key rules.
 */
public final class RouteCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteCatalog.class);

    /** One HTTP route and its key. {@code name} and {@code description} come only from {@link Permission}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Route(String method, String path, String key, boolean annotated, String name, String description) {

        String label() {
            return method + " " + path;
        }
    }

    private RouteCatalog() {
    }

    /**
     * @throws IllegalStateException when a {@link Permission} key is outside the service's namespace
     *                               or has an action outside {@link PermissionKeys#ACTIONS}, or when
     *                               two routes end up with the same key
     */
    public static List<Route> collect(String service, Map<RequestMappingInfo, HandlerMethod> handlers) {
        Map<String, Route> routes = new TreeMap<>();
        List<String> errors = new ArrayList<>();

        handlers.forEach((info, handler) -> {
            if (isRegistered(info, handler)) {
                addRoutes(service, info, handler, routes, errors);
            }
        });
        routes.values().stream()
                .filter(Route::annotated)
                .forEach(route -> checkAnnotatedKey(service, route, errors));
        checkOneRoutePerKey(routes, errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid permission keys in service '" + service + "':\n - "
                    + String.join("\n - ", errors));
        }
        return List.copyOf(routes.values());
    }

    /** Spring Boot's own handlers (such as /error), public routes and method-less mappings get no key. */
    private static boolean isRegistered(RequestMappingInfo info, HandlerMethod handler) {
        if (handler.getBeanType().getName().startsWith("org.springframework.") || isPublic(handler)) {
            return false;
        }
        if (info.getMethodsCondition().getMethods().isEmpty()) {
            LOGGER.warn("[Route Registration] {} maps no HTTP method and gets no permission key; "
                    + "declare the method (@GetMapping, @PostMapping, ...) to register it.", handler);
            return false;
        }
        return true;
    }

    /** One route per HTTP method and path of the handler; handlers sharing both (JSON and PDF) are one route. */
    private static void addRoutes(String service, RequestMappingInfo info, HandlerMethod handler,
                                  Map<String, Route> routes, List<String> errors) {
        Permission permission = handler.getMethodAnnotation(Permission.class);
        String name = permission == null ? null : blankToNull(permission.name());
        String description = permission == null ? null : blankToNull(permission.description());
        for (RequestMethod method : info.getMethodsCondition().getMethods()) {
            for (String path : info.getPatternValues()) {
                Route route = new Route(method.name(), path, keyFor(service, handler, method.name(), path),
                        permission != null, name, description);
                Route existing = routes.putIfAbsent(route.label(), route);
                if (existing != null && !existing.key().equals(route.key())) {
                    errors.add(route.label() + " has handlers with different keys: "
                            + existing.key() + " and " + route.key());
                }
            }
        }
    }

    private static void checkOneRoutePerKey(Map<String, Route> routes, List<String> errors) {
        routes.values().stream()
                .collect(Collectors.groupingBy(Route::key, TreeMap::new, Collectors.toList()))
                .forEach((key, sharing) -> {
                    if (sharing.size() > 1) {
                        errors.add("key " + key + " is used by more than one route: "
                                + sharing.stream().map(Route::label).collect(Collectors.joining(", "))
                                + ". Give each route its own @Permission key; a handler that maps several "
                                + "paths or methods under one @Permission must be split into one handler per route.");
                    }
                });
    }

    /**
     * The key of one route of a handler: its {@link Permission} key if it has one, otherwise the
     * key derived from the method and the matched path pattern. Registration and the per-request
     * check both use this, so they always agree.
     */
    public static String keyFor(String service, HandlerMethod handler, String method, String pathPattern) {
        Permission permission = handler.getMethodAnnotation(Permission.class);
        return permission != null ? permission.value() : PermissionKeys.derive(service, method, pathPattern);
    }

    /** {@link Public} on the handler method or on its controller. */
    public static boolean isPublic(HandlerMethod handler) {
        return handler.hasMethodAnnotation(Public.class)
                || AnnotatedElementUtils.hasAnnotation(handler.getBeanType(), Public.class);
    }

    private static void checkAnnotatedKey(String service, Route route, List<String> errors) {
        String key = route.key();
        String prefix = service + ".";
        if (!key.startsWith(prefix) || key.indexOf('.', prefix.length()) < 0) {
            errors.add(route.label() + ": @Permission(\"" + key + "\") must have the form "
                    + prefix + "{resource}.{action}, in this service's own namespace '" + service + "'");
        }
        String action = PermissionKeys.actionOf(key);
        if (!PermissionKeys.ACTIONS.contains(action)) {
            errors.add(route.label() + ": @Permission(\"" + key + "\") has action '" + action
                    + "', which is not one of " + new TreeSet<>(PermissionKeys.ACTIONS));
        }
    }

    private static String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }
}
