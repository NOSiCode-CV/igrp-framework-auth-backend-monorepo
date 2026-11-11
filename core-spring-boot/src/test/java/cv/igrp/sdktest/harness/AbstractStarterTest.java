package cv.igrp.sdktest.harness;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for tests that drive the starter over HTTP: {@link StarterTestApplication}
 * behind MockMvc, talking to a {@link StubAccessManagement} and a Redis 7 container.
 *
 * <p>The stub and the container live for the whole test JVM, like the cached Spring
 * context, so the requests the starter sends at startup stay visible to every test.
 * Needs Docker.
 */
@SpringBootTest(classes = StarterTestApplication.class)
@AutoConfigureMockMvc
public abstract class AbstractStarterTest {

    protected static final String SERVICE_NAME = "starter-test";
    protected static final String SERVICE_VERSION = "1.4.0";
    protected static final String CLIENT_ID = "starter-test-client";
    protected static final String CLIENT_SECRET = "starter-test-secret";
    protected static final String APPLICATION_CODE = "starter-app";

    /** Every registered route's key in {@link StarterTestApplication}. */
    protected static final List<String> ALL_KEYS = List.of(
            "starter-test.ping.list", "starter-test.notes.create", "starter-test.audit.commit.create",
            "starter-test.audit.rollback.create", "starter-test.correlation.list",
            "starter-test.correlation.call-am.list", "starter-test.faturas.list", "starter-test.faturas.read",
            "starter-test.faturas.aprovar.update", "starter-test.infra.status.list",
            "starter-test.legacy.guarded.list");

    protected static final StubAccessManagement AM = StubAccessManagement.start();

    @SuppressWarnings("resource")
    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @Autowired
    protected MockMvc mvc;

    @DynamicPropertySource
    static void starterProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.application.name", () -> SERVICE_NAME);
        registry.add("igrp.security.service-version", () -> SERVICE_VERSION);
        registry.add("igrp.access.api.base-url", AM::baseUrl);
        registry.add("igrp.access.m2m.client-id", () -> CLIENT_ID);
        registry.add("igrp.access.m2m.client-secret", () -> CLIENT_SECRET);
        // AM is the token issuer. MockMvc tests use jwt(), so the key set is never fetched.
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> AM.baseUrl() + "/oauth2/jwks");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        // v3-07: a Redis that stops answering falls back to AM quickly, not after Lettuce's 60 s.
        registry.add("spring.data.redis.timeout", () -> "2s");
        registry.add("igrp.security.application-code", () -> APPLICATION_CODE);
        registry.add("igrp.security.public-paths", () -> "/infra/**");
    }

    /**
     * By default AM grants every key of {@link StarterTestApplication}, so tests about other
     * features reach their routes. The route filter's own tests narrow it down.
     */
    @BeforeEach
    void grantEveryRoute() {
        AM.stub("GET", "/api/users/me/permissions", 200, permissionSet(ALL_KEYS));
    }

    protected static String permissionSet(List<String> keys) {
        return "{\"keys\":[" + keys.stream().map(key -> "\"" + key + "\"")
                .collect(Collectors.joining(",")) + "],\"scopes\":{}}";
    }
}
