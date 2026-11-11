package cv.igrp.framework.auth.core.route;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** v3-07: the AM side of the lookup when Redis has nothing to say, case by case. */
class PermissionLookupTest {

    private HttpServer am;

    private PermissionLookup lookupAnswering(int status, String body) throws IOException {
        am = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        am.createContext("/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        am.start();
        return new PermissionLookup(null,
                RestClient.create("http://localhost:" + am.getAddress().getPort()), "app");
    }

    @AfterEach
    void stop() {
        if (am != null) {
            am.stop(0);
        }
    }

    @Test
    @DisplayName("Without Redis configured, AM answers: the key is in the set")
    void allowedWithoutRedis() throws IOException {
        assertEquals(PermissionLookup.Answer.ALLOWED,
                lookupAnswering(200, "{\"keys\":[\"svc.a.list\"],\"scopes\":{}}").check("u", "t", "svc.a.list"));
    }

    @Test
    @DisplayName("A set without keys, or with no keys field: denied")
    void noKeys() throws IOException {
        assertEquals(PermissionLookup.Answer.DENIED, lookupAnswering(200, "{\"scopes\":{}}").check("u", "t", "svc.a.list"));
    }

    @Test
    @DisplayName("An application AM does not know (404) is a misconfiguration: unavailable, never allowed")
    void unknownApplication() throws IOException {
        assertEquals(PermissionLookup.Answer.UNAVAILABLE, lookupAnswering(404, "{}").check("u", "t", "svc.a.list"));
    }

    @Test
    @DisplayName("AM unreachable: unavailable")
    void unreachable() {
        PermissionLookup lookup = new PermissionLookup(null, RestClient.create("http://127.0.0.1:1"), "app");
        assertEquals(PermissionLookup.Answer.UNAVAILABLE, lookup.check("u", "t", "svc.a.list"));
    }
}
