package cv.igrp.framework.auth.core.exchange;

import org.springframework.security.oauth2.jwt.Jwt;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Bounded process-local cache; concurrent misses for the same context share one exchange. */
public final class ExchangedTokenCache {
    @FunctionalInterface
    public interface Exchange {
        Result exchange(Jwt subject, Jwt actor, String audience, Set<String> scopes);
    }
    public record Result(String accessToken, Instant expiresAt) {
        @Override public String toString() { return "ExchangedToken[redacted]"; }
    }
    private record Key(String subject, String actor, String audience, List<String> scopes) { }
    private final Map<Key, Result> cache = new LinkedHashMap<>(16, .75f, true);
    private final Object[] locks = new Object[64];
    private final Exchange exchange;
    private final Clock clock;
    private final Duration skew;
    private final int maximum;

    public ExchangedTokenCache(Exchange exchange, Clock clock, Duration skew, int maximum) {
        if (maximum < 1 || skew.isNegative()) throw new IllegalArgumentException("Invalid token cache limits");
        this.exchange = Objects.requireNonNull(exchange);
        this.clock = Objects.requireNonNull(clock);
        this.skew = skew;
        this.maximum = maximum;
        Arrays.setAll(locks, index -> new Object());
    }

    public String get(Jwt subject, Jwt actor, String audience, Set<String> scopes) {
        Objects.requireNonNull(subject);
        if (audience == null || audience.isBlank()) throw new IllegalArgumentException("Audience is required");
        Set<String> requested = Set.copyOf(scopes);
        Key key = key(subject, actor, audience, requested);
        synchronized (locks[Math.floorMod(key.hashCode(), locks.length)]) {
            Instant now = clock.instant();
            validateInput(subject, now);
            if (actor != null) validateInput(actor, now);
            synchronized (cache) {
                Result cached = cache.get(key);
                if (cached != null && now.plus(skew).isBefore(cached.expiresAt())) return cached.accessToken();
                cache.remove(key);
            }
            Result result = exchange.exchange(subject, actor, audience, requested);
            now = clock.instant();
            validateInput(subject, now);
            if (actor != null) validateInput(actor, now);
            if (result == null || result.accessToken() == null || result.accessToken().isBlank()
                    || result.expiresAt() == null) throw new IllegalStateException("Invalid token exchange response");
            Instant expiry = earlier(result.expiresAt(), subject.getExpiresAt());
            if (actor != null) expiry = earlier(expiry, actor.getExpiresAt());
            if (!expiry.isAfter(now)) throw new IllegalStateException("Exchanged token already expired");
            // Short-lived responses can serve this call, but must not be reused within the safety margin.
            if (now.plus(skew).isBefore(expiry)) {
                synchronized (cache) {
                    Instant cutoff = now.plus(skew);
                    cache.values().removeIf(value -> !cutoff.isBefore(value.expiresAt()));
                    cache.put(key, new Result(result.accessToken(), expiry));
                    while (cache.size() > maximum) cache.remove(cache.keySet().iterator().next());
                }
            }
            return result.accessToken();
        }
    }

    /** Evict a rejected token without removing a newer value installed by another request. */
    public void invalidate(Jwt subject, Jwt actor, String audience, Set<String> scopes, String rejected) {
        synchronized (cache) {
            Key key = key(subject, actor, audience, scopes);
            Result value = cache.get(key);
            if (value != null && value.accessToken().equals(rejected)) cache.remove(key);
        }
    }

    public void clear() { synchronized (cache) { cache.clear(); } }

    private static void validateInput(Jwt token, Instant now) {
        if (token.getSubject() == null || token.getExpiresAt() == null || !token.getExpiresAt().isAfter(now)
                || (token.getNotBefore() != null && token.getNotBefore().isAfter(now))) {
            throw new IllegalStateException("An active subject/actor token is required");
        }
    }
    private static Key key(Jwt subject, Jwt actor, String audience, Set<String> scopes) {
        // Fingerprints isolate user, issuer, session, role changes and token rotation without raw input tokens as keys.
        return new Key(fingerprint(subject.getTokenValue()), actor == null ? "" : fingerprint(actor.getTokenValue()),
                audience, scopes.stream().sorted().toList());
    }
    private static String fingerprint(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static Instant earlier(Instant a, Instant b) { return a.isBefore(b) ? a : b; }
}
