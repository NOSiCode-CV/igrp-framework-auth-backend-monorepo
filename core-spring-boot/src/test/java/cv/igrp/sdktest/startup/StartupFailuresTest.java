package cv.igrp.sdktest.startup;

import cv.igrp.sdktest.startup.action.ActionApp;
import cv.igrp.sdktest.startup.derivedreuse.DerivedReuseApp;
import cv.igrp.sdktest.startup.prefix.PrefixApp;
import cv.igrp.sdktest.startup.reuse.ReuseApp;
import cv.igrp.sdktest.startup.servicename.ServiceNameApp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

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
    @DisplayName("igrp.security.service-name wins over spring.application.name for the key prefix")
    void serviceNameProperty() {
        try (ConfigurableApplicationContext ignored = start(ServiceNameApp.class,
                "--igrp.security.service-name=billing")) {
            // started: billing.faturas.list is in the service's own namespace
        }
    }

    private static void assertStartupFails(Class<?> app, String... messageParts) {
        Throwable failure = assertThrows(Throwable.class, () -> start(app).close());
        String messages = messages(failure);
        for (String part : messageParts) {
            assertTrue(messages.contains(part), "expected '" + part + "' in: " + messages);
        }
    }

    private static ConfigurableApplicationContext start(Class<?> app, String... extraArgs) {
        String[] base = {
                "--spring.application.name=failing-test",
                "--server.port=0",
                // Unreachable on purpose: these apps must fail (or start) before AM matters.
                "--igrp.access.api.base-url=http://127.0.0.1:1",
                "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:1/oauth2/jwks",
        };
        String[] args = new String[base.length + extraArgs.length];
        System.arraycopy(base, 0, args, 0, base.length);
        System.arraycopy(extraArgs, 0, args, base.length, extraArgs.length);
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
