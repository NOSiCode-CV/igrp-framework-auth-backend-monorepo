package cv.igrp.sdktest.harness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The harness itself: a real application boots with the starter, answers over
 * HTTP, reaches a stubbed Access Management and a Redis container.
 */
class StarterHarnessTest extends AbstractStarterTest {

    @Autowired
    StringRedisTemplate redis;

    @Test
    @DisplayName("The test application serves its controller through MockMvc")
    void servesController() throws Exception {
        mvc.perform(get("/ping").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    @DisplayName("The starter loads through AutoConfiguration.imports and its calls reach the stubbed AM (alert A3)")
    void stubbedAccessManagement() {
        // StarterTestApplication lists no iGRP configuration and scans only its own package,
        // so these startup calls happen only if the starter arrived through the imports file.
        // The starter takes a client_credentials token, then syncs with it.
        StubAccessManagement.Request token = AM.lastRequest("POST", "/oauth2/token");
        assertEquals("Basic " + Base64.getEncoder().encodeToString(
                (CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8)), token.header("Authorization"));
        assertTrue(token.body().contains("grant_type=client_credentials"), token.body());

        StubAccessManagement.Request sync = AM.lastRequest("POST", "/api/m2m/sync/resources");
        assertEquals("Bearer " + StubAccessManagement.ACCESS_TOKEN, sync.header("Authorization"));
        assertTrue(sync.body().contains("\"name\":\"" + SERVICE_NAME + "\""), sync.body());
    }

    @Test
    @DisplayName("The application's Redis connection points at the test's Redis container")
    void redisContainer() throws Exception {
        String key = "harness:" + UUID.randomUUID();
        redis.opsForValue().set(key, "from-the-app");

        // Read inside the container, so the test fails if the app talks to any other Redis.
        assertEquals("from-the-app", REDIS.execInContainer("redis-cli", "GET", key).getStdout().strip());
    }
}
