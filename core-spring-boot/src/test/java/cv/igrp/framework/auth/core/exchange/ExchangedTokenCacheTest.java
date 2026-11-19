package cv.igrp.framework.auth.core.exchange;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ExchangedTokenCacheTest {
    private final MutableClock clock = new MutableClock();
    private final AtomicInteger calls = new AtomicInteger();
    private ExchangedTokenCache cache(int size) {
        return new ExchangedTokenCache((subject, actor, audience, scopes) ->
                new ExchangedTokenCache.Result("exchanged-" + calls.incrementAndGet(), clock.instant().plusSeconds(100)),
                clock, Duration.ofSeconds(10), size);
    }
    private Jwt token(String raw, String user) {
        return Jwt.withTokenValue(raw).header("alg", "RS256").subject(user)
                .expiresAt(clock.instant().plusSeconds(1000)).build();
    }
    @Test void repeatedCallsReuseTokenUntilRefreshMargin() {
        var cache = cache(10);
        var subject = token("subject", "user");
        String first = cache.get(subject, null, "b", Set.of("read"));
        assertEquals(first, cache.get(subject, null, "b", Set.of("read")));
        assertEquals(1, calls.get());
        clock.now = clock.now.plusSeconds(91);
        assertNotEquals(first, cache.get(subject, null, "b", Set.of("read")));
        assertEquals(2, calls.get());
    }
    @Test void usersTargetsScopesActorsAndRotatedSessionsAreIsolated() {
        var cache = cache(10);
        var subject = token("session-one", "user");
        Set<String> results = new HashSet<>();
        results.add(cache.get(subject, null, "b", Set.of("read")));
        results.add(cache.get(subject, null, "c", Set.of("read")));
        results.add(cache.get(subject, null, "b", Set.of("write")));
        results.add(cache.get(token("session-two", "user"), null, "b", Set.of("read")));
        results.add(cache.get(token("other", "other-user"), null, "b", Set.of("read")));
        results.add(cache.get(subject, token("actor", "agent"), "b", Set.of("read")));
        assertEquals(6, results.size());
    }
    @Test void rejectedTokenIsEvictedAndCapacityIsBounded() {
        var cache = cache(1);
        var subject = token("s", "u");
        String first = cache.get(subject, null, "b", Set.of());
        cache.invalidate(subject, null, "b", Set.of(), first);
        assertNotEquals(first, cache.get(subject, null, "b", Set.of()));
        cache.get(subject, null, "c", Set.of());
        cache.get(subject, null, "b", Set.of());
        assertEquals(4, calls.get());
    }
    @Test void expiredSubjectCannotReuseCachedToken() {
        var cache = cache(10);
        var subject = token("s", "u");
        cache.get(subject, null, "b", Set.of());
        clock.now = clock.now.plusSeconds(1001);
        Set<String> scopes = Set.of();
        assertThrows(IllegalStateException.class, () -> cache.get(subject, null, "b", scopes));
        assertEquals(1, calls.get());
    }
    @Test void concurrentCallsShareOneExchange() throws Exception {
        var cache = cache(10);
        var subject = token("s", "u");
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 20; i++) futures.add(executor.submit(() -> cache.get(subject, null, "b", Set.of())));
            for (var future : futures) assertEquals("exchanged-1", future.get(5, TimeUnit.SECONDS));
        }
        assertEquals(1, calls.get());
    }
    @Test void missingExpiryAndFutureNotBeforeAreRejectedBeforeExchange() {
        var cache = cache(10);
        var subject = token("valid", "user");
        var missingExpiry = Jwt.withTokenValue("missing").header("alg", "RS256").subject("user").build();
        var future = Jwt.withTokenValue("future").header("alg", "RS256").subject("user")
                .notBefore(clock.instant().plusSeconds(10)).expiresAt(clock.instant().plusSeconds(100)).build();
        Set<String> scopes = Set.of();
        for (Jwt invalid : List.of(missingExpiry, future)) {
            assertThrows(IllegalStateException.class, () -> cache.get(invalid, null, "b", scopes));
            assertThrows(IllegalStateException.class, () -> cache.get(subject, invalid, "b", scopes));
        }
        assertEquals(0, calls.get());
    }

    @Test void actorExpiryLimitsCacheReuse() {
        var cache = cache(10);
        var subject = token("s", "user");
        var actor = Jwt.withTokenValue("actor").header("alg", "RS256").subject("agent")
                .expiresAt(clock.instant().plusSeconds(30)).build();
        Set<String> scopes = Set.of();
        String first = cache.get(subject, actor, "b", scopes);
        clock.now = clock.now.plusSeconds(21);
        assertNotEquals(first, cache.get(subject, actor, "b", scopes));
        clock.now = clock.now.plusSeconds(10);
        assertThrows(IllegalStateException.class, () -> cache.get(subject, actor, "b", scopes));
        assertEquals(2, calls.get());
    }

    @Test void inputExpiryDuringExchangeIsRejected() {
        var subject = token("s", "user");
        var cache = new ExchangedTokenCache((s, a, audience, scopes) -> {
            clock.now = clock.now.plusSeconds(1001);
            return new ExchangedTokenCache.Result("exchanged", clock.instant().plusSeconds(100));
        }, clock, Duration.ZERO, 10);
        Set<String> scopes = Set.of();
        assertThrows(IllegalStateException.class, () -> cache.get(subject, null, "b", scopes));
    }

    private static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-19T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
