package cv.igrp.platform.access.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * v3-24 (#31): headers supplied once (the caller's trace and correlation ids)
 * go on every request, and a per-call header of the same name replaces them.
 */
class ApiClientDefaultHeadersTest {

    private HttpServer server;
    private final AtomicReference<Map<String, List<String>>> received = new AtomicReference<>();
    private ApiClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                received.set(Map.copyOf(exchange.getRequestHeaders()));
                exchange.sendResponseHeaders(200, -1);
            }
        });
        server.start();
        client = new ApiClient();
        client.setBaseUrl("http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void defaultHeadersAreReadOnEveryCall() throws Exception {
        String[] id = {"first"};
        client.setDefaultHeaders(() -> Map.of("X-Correlation-ID", id[0]));

        client.invokeAPIRaw("/echo", "GET", Map.of(), null, Map.of());
        assertEquals(List.of("first"), header("X-Correlation-ID"));

        id[0] = "second";
        client.invokeAPIStream("/echo", "GET", Map.of(), null).close();
        assertEquals(List.of("second"), header("X-Correlation-ID"));
    }

    @Test
    void aPerCallHeaderReplacesTheDefaultOfTheSameName() throws Exception {
        client.setDefaultHeaders(() -> Map.of("X-Correlation-ID", "default", "traceparent", "00-t-s-01"));

        client.invokeAPIRaw("/echo", "GET", Map.of(), null, Map.of("x-correlation-id", "explicit"));

        assertEquals(List.of("explicit"), header("X-Correlation-ID"));
        assertEquals(List.of("00-t-s-01"), header("Traceparent"));
    }

    @Test
    void withoutDefaultHeadersNothingIsAdded() throws Exception {
        client.setDefaultHeaders(null);

        client.invokeAPIRaw("/echo", "GET", Map.of(), null, Map.of());

        assertNull(header("X-Correlation-ID"));
    }

    @Test
    void aSupplierThatReturnsNullAddsNothing() throws Exception {
        client.setDefaultHeaders(() -> null);

        client.invokeAPIRaw("/echo", "GET", Map.of(), null, Map.of());

        assertNull(header("X-Correlation-ID"));
    }

    private List<String> header(String name) {
        return received.get().entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
