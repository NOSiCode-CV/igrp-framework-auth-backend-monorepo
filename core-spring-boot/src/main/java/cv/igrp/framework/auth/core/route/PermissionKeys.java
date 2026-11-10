package cv.igrp.framework.auth.core.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The key convention, {@code {service}.{resource path}.{action}}: one pure function,
 * used both to register routes and to check each request, so the two always agree.
 */
public final class PermissionKeys {

    /** The closed action vocabulary: the method/ending table plus the other HTTP methods in lowercase. */
    public static final Set<String> ACTIONS = Set.of(
            "list", "read", "create", "update-all", "update", "patch-all", "patch", "delete-all", "delete",
            "head", "options", "trace");

    private static final Pattern VERSION = Pattern.compile("v\\d+");

    private PermissionKeys() {
    }

    /**
     * @param service     the service name, the first segment of the key
     * @param method      the HTTP method, any case
     * @param pathPattern the route's path pattern, e.g. {@code /api/v1/faturas/{id}/linhas}
     */
    public static String derive(String service, String method, String pathPattern) {
        List<String> segments = new ArrayList<>();
        for (String segment : pathPattern.split("/")) {
            if (!segment.isEmpty()) {
                segments.add(segment);
            }
        }
        if (!segments.isEmpty() && segments.getFirst().equals("api")) {
            segments.removeFirst();
        }
        if (!segments.isEmpty() && VERSION.matcher(segments.getFirst()).matches()) {
            segments.removeFirst();
        }

        boolean endsInVariable = !segments.isEmpty() && isVariable(segments.getLast());
        List<String> resources = segments.stream().filter(s -> !isVariable(s)).toList();
        String resourcePath = resources.isEmpty() ? "root" : String.join(".", resources);

        return service + "." + resourcePath + "." + action(method, endsInVariable);
    }

    /** The action of a key: its last segment. */
    public static String actionOf(String key) {
        return key.substring(key.lastIndexOf('.') + 1);
    }

    private static String action(String method, boolean endsInVariable) {
        String m = method.toUpperCase(Locale.ROOT);
        return switch (m) {
            case "GET" -> endsInVariable ? "read" : "list";
            case "POST" -> "create";
            case "PUT" -> endsInVariable ? "update" : "update-all";
            case "PATCH" -> endsInVariable ? "patch" : "patch-all";
            case "DELETE" -> endsInVariable ? "delete" : "delete-all";
            default -> m.toLowerCase(Locale.ROOT);
        };
    }

    // {id}, {*rest} and the ** / * wildcards stand for values, not resources.
    private static boolean isVariable(String segment) {
        return segment.startsWith("{") || segment.equals("*") || segment.equals("**");
    }
}
