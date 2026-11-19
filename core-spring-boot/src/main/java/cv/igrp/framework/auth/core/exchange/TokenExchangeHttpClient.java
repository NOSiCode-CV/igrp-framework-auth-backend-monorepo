package cv.igrp.framework.auth.core.exchange;

import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** RFC 8693 transport. Never logs credentials, input tokens or response bodies. */
public final class TokenExchangeHttpClient implements ExchangedTokenCache.Exchange {
    private static final String ACCESS_TOKEN = "access_token";
    private static final String EXPIRES_IN = "expires_in";
    private final URI endpoint;
    private final String authorization;
    private final Duration timeout;
    private final Clock clock;
    private final HttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public TokenExchangeHttpClient(URI endpoint, String clientId, String secret, Duration timeout, Clock clock) {
        if (endpoint == null || endpoint.getHost() == null || endpoint.getUserInfo() != null
                || !("https".equals(endpoint.getScheme()) || "http".equals(endpoint.getScheme()))
                || endpoint.getFragment() != null || timeout.isNegative() || timeout.isZero()
                || clientId == null || clientId.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Valid token endpoint, client credentials and timeout are required");
        }
        this.endpoint = endpoint;
        this.authorization = "Basic " + Base64.getEncoder().encodeToString(
                (encode(clientId) + ":" + encode(secret)).getBytes(StandardCharsets.UTF_8));
        this.timeout = timeout;
        this.clock = clock;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public ExchangedTokenCache.Result exchange(Jwt subject, Jwt actor, String audience, Set<String> scopes) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        form.put("subject_token", subject.getTokenValue());
        form.put("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        form.put("audience", audience);
        if (!scopes.isEmpty()) form.put("scope", String.join(" ", new TreeSet<>(scopes)));
        if (actor != null) {
            form.put("actor_token", actor.getTokenValue());
            form.put("actor_token_type", "urn:ietf:params:oauth:token-type:access_token");
        }
        String body = form.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
        Instant started = clock.instant(); // Network latency must not extend token lifetime.
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(timeout).header("Authorization", authorization)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("AM token exchange failed (HTTP " + response.statusCode() + ")");
            }
            var value = json.readTree(response.body());
            if (!value.hasNonNull(ACCESS_TOKEN) || !value.get(ACCESS_TOKEN).isString()
                    || !value.hasNonNull("token_type") || !"Bearer".equalsIgnoreCase(value.get("token_type").asString())
                    || !value.hasNonNull(EXPIRES_IN) || !value.get(EXPIRES_IN).isIntegralNumber()
                    || value.get(EXPIRES_IN).asLong() <= 0) {
                throw new IllegalStateException("Invalid AM token exchange response");
            }
            return new ExchangedTokenCache.Result(value.get(ACCESS_TOKEN).asString(),
                    started.plusSeconds(value.get(EXPIRES_IN).asLong()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Token exchange interrupted");
        } catch (java.io.IOException | tools.jackson.core.JacksonException ex) {
            throw new IllegalStateException("Token exchange transport or response failure");
        }
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
