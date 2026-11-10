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
            if (handler.getBeanType().getName().startsWith("org.springframework.")) {
                return; // Spring Boot's own handlers, such as /error
            }
            if (handler.hasMethodAnnotation(Public.class)
                    || AnnotatedElementUtils.hasAnnotation(handler.getBeanType(), Public.class)) {
                return;
            }
            if (info.getMethodsCondition().getMethods().isEmpty()) {
                LOGGER.warn("[Route Registration] {} maps no HTTP method and gets no permission key; "
                        + "declare the method (@GetMapping, @PostMapping, ...) to register it.", handler);
                return;
            }
            Permission permission = handler.getMethodAnnotation(Permission.class);
            for (RequestMethod method : info.getMethodsCondition().getMethods()) {
                for (String path : info.getPatternValues()) {
                    Route route = permission == null
                            ? new Route(method.name(), path, PermissionKeys.derive(service, method.name(), path),
                                    false, null, null)
                            : new Route(method.name(), path, permission.value(), true,
                                    blankToNull(permission.name()), blankToNull(permission.description()));
                    // Handlers sharing a method and path (JSON and PDF output) are one route.
                    Route existing = routes.putIfAbsent(route.label(), route);
                    if (existing != null && !existing.key().equals(route.key())) {
                        errors.add(route.label() + " has handlers with different keys: "
                                + existing.key() + " and " + route.key());
                    }
                }
            }
        });

        routes.values().stream()
                .filter(Route::annotated)
                .forEach(route -> checkAnnotatedKey(service, route, errors));
        routes.values().stream()
                .collect(Collectors.groupingBy(Route::key, TreeMap::new, Collectors.toList()))
                .forEach((key, sharing) -> {
                    if (sharing.size() > 1) {
                        errors.add("key " + key + " is used by more than one route: "
                                + sharing.stream().map(Route::label).collect(Collectors.joining(", "))
                                + ". Give each route its own @Permission key.");
                    }
                });

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid permission keys in service '" + service + "':\n - "
                    + String.join("\n - ", errors));
        }
        return List.copyOf(routes.values());
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
