package cv.igrp.sdktest.startup;

import cv.igrp.sdktest.startup.action.ActionApp;
import cv.igrp.sdktest.startup.derivedreuse.DerivedReuseApp;
import cv.igrp.sdktest.startup.prefix.PrefixApp;
import cv.igrp.sdktest.startup.reuse.ReuseApp;
import cv.igrp.sdktest.startup.servicename.ServiceNameApp;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The starter refuses to start a service whose routes break the key rules.
 * Each case is a real application in its own package, started the way a service starts.
 */
class StartupFailuresTest {

    @Test
    @DisplayName("Refuses to start when a @Permission key is outside the service's namespace")
    void prefix() {
        assertStartupFails(PrefixApp.class, "other-service.faturas.list", "failing-test");
    }

    @Test
    @DisplayName("Refuses to start when two routes declare the same @Permission key")
    void reuse() {
        assertStartupFails(ReuseApp.class, "failing-test.faturas.list", "GET /faturas", "GET /notas");
    }

    @Test
    @DisplayName("Refuses to start when two routes derive the same key")
    void derivedReuse() {
        assertStartupFails(DerivedReuseApp.class, "failing-test.faturas.list", "GET /faturas", "GET /api/faturas");
    }

    @Test
    @DisplayName("Refuses to start when a @Permission action is outside the allowed set")
    void action() {
        assertStartupFails(ActionApp.class, "failing-test.faturas.approve", "approve");
    }

    @Test
    @DisplayName("Refuses to start without a service version, which AM needs to gate deletes")
    void missingVersion() {
        String[] args = Stream.concat(Arrays.stream(BASE_ARGS_WITHOUT_VERSION),
                Stream.of("--igrp.security.service-name=billing")).toArray(String[]::new);
        assertStartupFails(ServiceNameApp.class, args, "igrp.security.service-version");
    }

    @Test
    @DisplayName("igrp.security.service-name wins over spring.application.name for the key prefix")
    void serviceNameProperty() {
        try (ConfigurableApplicationContext context = start(ServiceNameApp.class,
                "--igrp.security.service-name=billing")) {
            // billing.faturas.list is in the service's own namespace, so it starts
            assertTrue(context.isActive());
        }
    }

    @Test
    @DisplayName("An AM that accepts the registration and never answers does not hold the startup")
    void hangingAccessManagement() throws IOException {
        HttpServer am = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        am.setExecutor(Executors.newCachedThreadPool());
        am.createContext("/oauth2/token", exchange -> respond(exchange,
                "{\"access_token\":\"t\",\"token_type\":\"Bearer\",\"expires_in\":300}"));
        am.createContext("/api/routes/register", exchange -> {
            try {
                Thread.sleep(Duration.ofSeconds(30));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        am.start();
        String baseUrl = "http://127.0.0.1:" + am.getAddress().getPort();
        long started = System.nanoTime();
        try (ConfigurableApplicationContext context = start(ServiceNameApp.class,
                "--igrp.security.service-name=billing",
                "--igrp.access.api.base-url=" + baseUrl,
                "--igrp.access.m2m.client-id=c", "--igrp.access.m2m.client-secret=s",
                "--igrp.security.registration-timeout=1s")) {
            assertTrue(context.isActive());
            Duration took = Duration.ofNanos(System.nanoTime() - started);
            assertTrue(took.compareTo(Duration.ofSeconds(20)) < 0, "startup took " + took);
        } finally {
            am.stop(0);
        }
    }

    private static void respond(HttpExchange exchange, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static void assertStartupFails(Class<?> app, String... messageParts) {
        assertStartupFails(app, BASE_ARGS, messageParts);
    }

    private static void assertStartupFails(Class<?> app, String[] baseArgs, String... messageParts) {
        Throwable failure = assertThrows(Throwable.class, () -> run(app, baseArgs).close());
        String messages = messages(failure);
        for (String part : messageParts) {
            assertTrue(messages.contains(part), "expected '" + part + "' in: " + messages);
        }
    }

    private static final String[] BASE_ARGS_WITHOUT_VERSION = {
            "--spring.application.name=failing-test",
            "--server.port=0",
            // Unreachable on purpose: these apps must fail (or start) before AM matters.
            "--igrp.access.api.base-url=http://127.0.0.1:1",
            "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:1/oauth2/jwks",
            // Under surefire, Spring Boot reads this from the test runner's jar; blank it.
            "--spring.application.version=",
    };

    private static final String[] BASE_ARGS =
            Stream.concat(Arrays.stream(BASE_ARGS_WITHOUT_VERSION), Stream.of("--igrp.security.service-version=1.0.0"))
                    .toArray(String[]::new);

    private static ConfigurableApplicationContext start(Class<?> app, String... extraArgs) {
        return run(app, BASE_ARGS, extraArgs);
    }

    // Later arguments win, so extraArgs can override the base ones.
    private static ConfigurableApplicationContext run(Class<?> app, String[] baseArgs, String... extraArgs) {
        String[] args = Stream.concat(Arrays.stream(baseArgs), Arrays.stream(extraArgs)).toArray(String[]::new);
        return new SpringApplicationBuilder(app).run(args);
    }

    private static String messages(Throwable failure) {
        StringBuilder all = new StringBuilder();
        for (Throwable t = failure; t != null; t = t.getCause()) {
            all.append(t.getMessage()).append('\n');
        }
        return all.toString();
    }
}
