package cv.igrp.framework.auth.autentika.client;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

/** Issues and caches gateway client-credentials tokens, renewing them before expiry. */
public final class AutentikaTokenProvider implements Supplier<String> {
    private final URI endpoint;
    private final String authorization;
    private final Duration timeout;
    private final HttpClient http;
    private final Clock clock;
    private final JsonMapper json = JsonMapper.builder().build();
    private String accessToken;
    private Instant renewAt = Instant.MIN;

    public AutentikaTokenProvider(URI tokenEndpoint, String consumerKey, String consumerSecret, Duration timeout) {
        this(tokenEndpoint, consumerKey, consumerSecret, timeout,
                HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build(),
                Clock.systemUTC());
    }

    AutentikaTokenProvider(URI tokenEndpoint, String consumerKey, String consumerSecret, Duration timeout,
                          HttpClient http, Clock clock) {
        Objects.requireNonNull(tokenEndpoint, "tokenEndpoint");
        if (tokenEndpoint.getHost() == null
                || !("https".equals(tokenEndpoint.getScheme()) || "http".equals(tokenEndpoint.getScheme()))
                || tokenEndpoint.getQuery() != null || tokenEndpoint.getFragment() != null
                || tokenEndpoint.getUserInfo() != null) {
            throw new IllegalArgumentException("tokenEndpoint must be an HTTP(S) URL without credentials, query or fragment");
        }
        if (consumerKey == null || consumerKey.isBlank() || consumerSecret == null || consumerSecret.isBlank()) {
            throw new IllegalArgumentException("Consumer key and secret are required");
        }
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("timeout must be positive");
        this.endpoint = URI.create(tokenEndpoint + "?grant_type=client_credentials");
        String credentials = URLEncoder.encode(consumerKey, StandardCharsets.UTF_8) + ":"
                + URLEncoder.encode(consumerSecret, StandardCharsets.UTF_8);
        this.authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        this.http = Objects.requireNonNull(http, "http");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Fetches lazily; concurrent callers share one token request. Never logs credentials or tokens. */
    @Override
    public synchronized String get() {
        Instant requestedAt = clock.instant();
        if (accessToken != null && requestedAt.isBefore(renewAt)) return accessToken;
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                .header("Authorization", authorization).header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Autentika gateway token request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not reach the Autentika gateway token endpoint", e);
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Autentika gateway token request failed with HTTP " + response.statusCode());
        }
        String token;
        Instant nextRenewal;
        try {
            var body = json.readTree(response.body());
            var value = body.path("access_token");
            if (!value.isString() || value.asString().isBlank()
                    || value.asString().contains("\r") || value.asString().contains("\n")) {
                throw new IllegalArgumentException();
            }
            if (!"Bearer".equalsIgnoreCase(body.path("token_type").asString())) throw new IllegalArgumentException();
            long lifetime = Long.parseLong(body.path("expires_in").asString());
            if (lifetime <= 0) throw new IllegalArgumentException();
            nextRenewal = requestedAt.plusSeconds(lifetime - Math.min(30, lifetime / 10));
            if (!clock.instant().isBefore(nextRenewal)) throw new IllegalArgumentException();
            token = value.asString();
        } catch (RuntimeException e) {
            // Parsing exceptions may contain the response body, so do not retain them.
            throw new IllegalStateException("Autentika gateway returned an invalid token response");
        }
        accessToken = token;
        renewAt = nextRenewal;
        return accessToken;
    }
}
