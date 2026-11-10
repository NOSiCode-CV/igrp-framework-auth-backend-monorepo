package cv.igrp.framework.auth.core.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The key convention, {@code {service}.{resource path}.{action}}: one pure function,
 * used both to register routes and to check each request, so the two always agree.
 */
public final class PermissionKeys {

    /** The closed action vocabulary: the method/ending table plus the other HTTP methods in lowercase. */
    public static final Set<String> ACTIONS = Set.of(
            "list", "read", "create", "update-all", "update", "patch-all", "patch", "delete-all", "delete",
            "head", "options", "trace");

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
        if (!segments.isEmpty() && isVersion(segments.getFirst())) {
            segments.removeFirst();
        }

        boolean endsInVariable = !segments.isEmpty() && hasVariable(segments.getLast());
        List<String> resources = segments.stream()
                .map(PermissionKeys::literalPart)
                .filter(s -> !s.isEmpty())
                .toList();
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

    // {id}, {id:\d+}, {*rest} and the * / ** wildcards stand for values, not resources,
    // also inside a segment (relatorio-{ano}, *.pdf). Such a segment names one item.
    private static boolean hasVariable(String segment) {
        return segment.indexOf('{') >= 0 || segment.indexOf('*') >= 0;
    }

    /**
     * What a segment says about the resource: its text outside {@code {...}} (braces may nest,
     * as in {@code {id:\d{3}}}), with dots and wildcards turned into hyphens and separators
     * trimmed from both ends. A plain loop, so a route pattern cannot make it backtrack.
     */
    private static String literalPart(String segment) {
        StringBuilder literal = new StringBuilder(segment.length());
        int depth = 0;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth = Math.max(0, depth - 1);
            } else if (depth == 0) {
                literal.append(c == '.' || c == '*' ? '-' : c);
            }
        }
        int start = 0;
        int end = literal.length();
        while (start < end && !Character.isLetterOrDigit(literal.charAt(start))) {
            start++;
        }
        while (end > start && !Character.isLetterOrDigit(literal.charAt(end - 1))) {
            end--;
        }
        return literal.substring(start, end);
    }

    private static boolean isVersion(String segment) {
        if (segment.length() < 2 || segment.charAt(0) != 'v') {
            return false;
        }
        for (int i = 1; i < segment.length(); i++) {
            if (!Character.isDigit(segment.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
