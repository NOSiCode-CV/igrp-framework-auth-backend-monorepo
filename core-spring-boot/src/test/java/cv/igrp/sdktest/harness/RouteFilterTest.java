package cv.igrp.sdktest.harness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v3-07 (#15): every route is closed unless the caller's permission set has its key.
 * The set is the one AM writes in Redis; a miss asks AM with the caller's own token.
 * Each protection is shown refusing the unprotected case (alert A5).
 */
@ExtendWith(OutputCaptureExtension.class)
class RouteFilterTest extends AbstractStarterTest {

    private static final String PERMISSIONS = "/api/users/me/permissions";

    @Autowired
    StringRedisTemplate redis;

    /** Writes the set as AM does: the keys plus the marker, which makes an empty set a hit. */
    private String cachedUser(String... keys) {
        String sub = "user-" + UUID.randomUUID();
        redis.opsForSet().add("permissions:user:" + sub + ":app:" + APPLICATION_CODE,
                Stream.concat(Stream.of("~"), Stream.of(keys)).toArray(String[]::new));
        return sub;
    }

    private static RequestPostProcessor token(String sub) {
        return jwt().jwt(jwt -> jwt.subject(sub));
    }

    private static long fetches() {
        return AM.count("GET", PERMISSIONS);
    }

    private static long redisChecks() throws Exception {
        String stats = REDIS.execInContainer("redis-cli", "INFO", "commandstats").getStdout();
        return stats.lines().filter(line -> line.startsWith("cmdstat_smismember:")).findFirst()
                .map(line -> Long.parseLong(line.replaceAll(".*calls=(\\d+),.*", "$1"))).orElse(0L);
    }

    @Test
    @DisplayName("A key in the Redis set: 200, and AM is not asked")
    void allowedFromRedis() throws Exception {
        String sub = cachedUser("starter-test.faturas.list");
        long before = fetches();

        mvc.perform(get("/api/v1/faturas").with(token(sub))).andExpect(status().isOk());

        assertEquals(before, fetches());
    }

    @Test
    @DisplayName("A key missing from the Redis set: 403 and an audited denial, without asking AM")
    void deniedFromRedis(CapturedOutput output) throws Exception {
        String sub = cachedUser("starter-test.ping.list");
        long before = fetches();

        mvc.perform(get("/api/v1/faturas").with(token(sub))).andExpect(status().isForbidden());

        assertEquals(before, fetches());
        assertTrue(output.getOut().contains("access.denied GET /api/v1/faturas"), "denial not audited");
        assertTrue(output.getOut().contains("starter-test.faturas.list"));
    }

    @Test
    @DisplayName("A set with only the marker is a hit with no permissions: 403, AM is not asked")
    void emptySetIsAHit() throws Exception {
        String sub = cachedUser();
        long before = fetches();

        mvc.perform(get("/ping").with(token(sub))).andExpect(status().isForbidden());

        assertEquals(before, fetches());
    }

    @Test
    @DisplayName("Anonymous on a protected route: 401, with no Redis lookup and no call to AM")
    void anonymous() throws Exception {
        long fetchesBefore = fetches();
        long redisBefore = redisChecks();

        mvc.perform(get("/api/v1/faturas")).andExpect(status().isUnauthorized());

        assertEquals(fetchesBefore, fetches());
        assertEquals(redisBefore, redisChecks());
    }

    @Test
    @DisplayName("@Public and igrp.security.public-paths: 200 without a token; their neighbours stay closed")
    void publicRoutes() throws Exception {
        mvc.perform(get("/api/v1/faturas/publicas")).andExpect(status().isOk());
        mvc.perform(get("/infra/status")).andExpect(status().isOk());

        mvc.perform(get("/api/v1/faturas/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/ping")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An unknown path: 404 for a caller with a token, 401 for an anonymous one")
    void unknownPath() throws Exception {
        mvc.perform(get("/does-not-exist").with(token(cachedUser()))).andExpect(status().isNotFound());
        mvc.perform(get("/does-not-exist")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Root: allowed without any lookup, and audited")
    void root(CapturedOutput output) throws Exception {
        AM.stub("GET", PERMISSIONS, 200, permissionSet(List.of()));
        long before = fetches();

        mvc.perform(get("/api/v1/faturas").with(jwt().jwt(jwt -> jwt.subject("root-" + UUID.randomUUID())
                .claim("is_super_admin", true)))).andExpect(status().isOk());

        assertEquals(before, fetches());
        assertTrue(output.getOut().contains("access.root GET /api/v1/faturas"), "root access not audited");
    }

    @Test
    @DisplayName("A miss asks AM with the caller's own token and the application code")
    void missAsksAm() throws Exception {
        mvc.perform(get("/api/v1/faturas").with(jwt().jwt(jwt -> jwt.subject("user-" + UUID.randomUUID())
                .tokenValue("callers-own-token")))).andExpect(status().isOk());

        StubAccessManagement.Request fetch = AM.lastRequest("GET", PERMISSIONS);
        assertEquals("Bearer callers-own-token", fetch.header("Authorization"));
        assertEquals("applicationCode=" + APPLICATION_CODE, fetch.query());
    }

    @Test
    @DisplayName("A miss where AM's set lacks the key: 403")
    void missDenied() throws Exception {
        AM.stub("GET", PERMISSIONS, 200, permissionSet(List.of("starter-test.ping.list")));

        mvc.perform(get("/api/v1/faturas").with(token("user-" + UUID.randomUUID()))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AM no longer authorizes the caller (401 from the fetch): 401")
    void amRefusesTheCaller() throws Exception {
        AM.stub("GET", PERMISSIONS, 401, "{}");

        mvc.perform(get("/api/v1/faturas").with(token("user-" + UUID.randomUUID()))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Neither Redis nor AM can answer: 503, never an allow")
    void amUnavailable() throws Exception {
        AM.stub("GET", PERMISSIONS, 500, "{}");

        mvc.perform(get("/api/v1/faturas").with(token("user-" + UUID.randomUUID())))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("A HEAD on a GET route needs the GET key")
    void headUsesTheGetKey() throws Exception {
        mvc.perform(head("/api/v1/faturas/7").with(token(cachedUser("starter-test.faturas.read"))))
                .andExpect(status().isOk());
        mvc.perform(head("/api/v1/faturas/7").with(token(cachedUser("starter-test.faturas.list"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A handler that is not a route of the service (Spring's /error) is closed")
    void notARoute() throws Exception {
        mvc.perform(get("/error").with(token(cachedUser(ALL_KEYS.toArray(String[]::new)))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A login that is not a bearer token has no permission set: 403")
    void notABearerToken() throws Exception {
        mvc.perform(get("/ping").with(user("maria"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Redis stops answering: the starter asks AM on every request, and still decides")
    void redisOutage() throws Exception {
        String sub = cachedUser("starter-test.ping.list");
        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            long before = fetches();
            mvc.perform(get("/ping").with(token(sub))).andExpect(status().isOk());
            assertEquals(before + 1, fetches());

            AM.stub("GET", PERMISSIONS, 200, permissionSet(List.of()));
            mvc.perform(get("/ping").with(token(sub))).andExpect(status().isForbidden());
        } finally {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }
    }
}
