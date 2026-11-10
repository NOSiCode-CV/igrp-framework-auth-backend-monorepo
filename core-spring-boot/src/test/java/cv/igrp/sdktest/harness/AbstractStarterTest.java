package cv.igrp.sdktest.harness;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

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
    protected static final String CLIENT_ID = "starter-test-client";
    protected static final String CLIENT_SECRET = "starter-test-secret";

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
        registry.add("igrp.access.api.base-url", AM::baseUrl);
        registry.add("igrp.access.m2m.client-id", () -> CLIENT_ID);
        registry.add("igrp.access.m2m.client-secret", () -> CLIENT_SECRET);
        // AM is the token issuer. MockMvc tests use jwt(), so the key set is never fetched.
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> AM.baseUrl() + "/oauth2/jwks");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
