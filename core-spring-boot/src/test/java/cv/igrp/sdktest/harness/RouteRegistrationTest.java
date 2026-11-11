package cv.igrp.sdktest.harness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * At startup the starter registers every route of {@link StarterTestApplication}
 * with AM: {@code POST /api/routes/register}, with the service's own token.
 */
class RouteRegistrationTest extends AbstractStarterTest {

    @Test
    @DisplayName("Startup registers the service's routes with AM, with keys derived or fixed by @Permission")
    void registersRoutes() {
        StubAccessManagement.Request register = AM.lastRequest("POST", "/api/routes/register");
        assertEquals("Bearer " + StubAccessManagement.ACCESS_TOKEN, register.header("Authorization"));

        JsonNode body = JsonMapper.builder().build().readTree(register.body());
        assertEquals(SERVICE_NAME, body.get("service").asString());
        assertEquals(SERVICE_VERSION, body.get("version").asString());

        Set<Map<String, Object>> routes = new HashSet<>();
        body.get("routes").forEach(route -> routes.add(asMap(route)));

        // The two GET /api/v1/faturas handlers (JSON and PDF) are one route.
        // Not registered: the @Public route and Spring Boot's own /error.
        assertEquals(Set.of(
                route("GET", "/ping", "starter-test.ping.list"),
                // Public by public-paths at request time, but still a route of the service.
                route("GET", "/infra/status", "starter-test.infra.status.list"),
                route("POST", "/notes", "starter-test.notes.create"),
                route("POST", "/audit/commit", "starter-test.audit.commit.create"),
                route("POST", "/audit/rollback", "starter-test.audit.rollback.create"),
                route("GET", "/correlation", "starter-test.correlation.list"),
                route("GET", "/correlation/call-am", "starter-test.correlation.call-am.list"),
                route("GET", "/api/v1/faturas", "starter-test.faturas.list"),
                route("GET", "/api/v1/faturas/{id}", "starter-test.faturas.read"),
                annotatedRoute("POST", "/api/v1/faturas/{id}/aprovar", "starter-test.faturas.aprovar.update",
                        "Aprovar fatura", "Aprova uma fatura pendente")
        ), routes);
    }

    private static Map<String, Object> route(String method, String path, String key) {
        return Map.of("method", method, "path", path, "key", key, "annotated", false);
    }

    private static Map<String, Object> annotatedRoute(String method, String path, String key,
                                                      String name, String description) {
        return Map.of("method", method, "path", path, "key", key, "annotated", true,
                "name", name, "description", description);
    }

    private static Map<String, Object> asMap(JsonNode route) {
        Map<String, Object> map = new LinkedHashMap<>();
        route.properties().forEach(field -> map.put(field.getKey(),
                field.getValue().isBoolean() ? field.getValue().asBoolean() : field.getValue().asString()));
        return map;
    }
}
