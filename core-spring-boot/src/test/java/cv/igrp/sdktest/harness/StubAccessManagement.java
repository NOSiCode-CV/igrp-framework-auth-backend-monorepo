package cv.igrp.sdktest.harness;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An Access Management stand-in on a local port, built on the JDK's HTTP server
 * so the harness adds no dependency. It records every request the starter sends
 * and answers with a canned response: a client_credentials token on
 * {@code POST /oauth2/token}, {@code 200 {}} on anything else, unless a test
 * calls {@link #stub}.
 */
public final class StubAccessManagement {

    public static final String ACCESS_TOKEN = "stub-am-token";

    private static final String TOKEN_RESPONSE =
            "{\"access_token\":\"" + ACCESS_TOKEN + "\",\"token_type\":\"Bearer\",\"expires_in\":300}";

    /** One request as AM received it. Header lookup ignores case. */
    public record Request(String method, String path, String query, Map<String, List<String>> headers, String body) {

        public String header(String name) {
            List<String> values = headers.get(name);
            return values == null || values.isEmpty() ? null : values.getFirst();
        }
    }

    private record Response(int status, String body) {
    }

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private final Map<String, Response> responses = new ConcurrentHashMap<>();

    private StubAccessManagement(HttpServer server) {
        this.server = server;
        stub("POST", "/oauth2/token", 200, TOKEN_RESPONSE);
        server.createContext("/", this::handle);
        server.start();
    }

    public static StubAccessManagement start() {
        try {
            return new StubAccessManagement(HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    /** Answers {@code method path} with this status and JSON body from now on. */
    public void stub(String method, String path, int status, String jsonBody) {
        responses.put(method + " " + path, new Response(status, jsonBody));
    }

    /** How many {@code method path} requests AM has received so far. */
    public long count(String method, String path) {
        return requests.stream().filter(r -> r.method().equals(method) && r.path().equals(path)).count();
    }

    /** The most recent {@code method path} request; fails listing what was received when there is none. */
    public Request lastRequest(String method, String path) {
        return requests.reversed().stream()
                .filter(r -> r.method().equals(method) && r.path().equals(path))
                .findFirst()
                .orElseThrow(() -> new AssertionError("AM received no " + method + " " + path + "; it received "
                        + requests.stream().map(r -> r.method() + " " + r.path()).toList()));
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            headers.putAll(exchange.getRequestHeaders());
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(new Request(method, path, exchange.getRequestURI().getQuery(), headers, body));

            Response response = responses.getOrDefault(method + " " + path, new Response(200, "{}"));
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.status(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
        }
    }
}
