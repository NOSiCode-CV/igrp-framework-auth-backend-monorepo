package cv.igrp.platform.access.client.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.errors.ProblemDetail;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Fetches and caches an OAuth2 {@code client_credentials} access token from the
 * Access Management API's authorization server endpoint
 * ({@code POST {baseUrl}/oauth2/token}).
 *
 * <p>One instance per (baseUrl, clientId) is enough for an application — it
 * memoises the token until it's within {@link #refreshLeewaySeconds} of
 * expiration, refreshing transparently on the next {@link #getAccessToken()}
 * call. Thread-safe.
 *
 * <p>This class centralises what was previously done implicitly by every
 * caller setting a static M2M header on the request. Library consumers
 * configure it once on the {@code ApiClient} via
 * {@code apiClient.setClientCredentials(clientId, clientSecret)} and the
 * client transparently injects {@code Authorization: Bearer <token>} on
 * every M2M call.
 */
public class ClientCredentialsTokenProvider {

    private static final String TOKEN_ENDPOINT_PATH = "/oauth2/token";
    private static final long DEFAULT_REFRESH_LEEWAY_SECONDS = 30;

    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final long refreshLeewaySeconds;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Object lock = new Object();
    private volatile String cachedAccessToken;
    private volatile Instant cachedExpiresAt;

    public ClientCredentialsTokenProvider(String baseUrl, String clientId, String clientSecret) {
        this(baseUrl, clientId, clientSecret, null, DEFAULT_REFRESH_LEEWAY_SECONDS);
    }

    public ClientCredentialsTokenProvider(String baseUrl,
                                          String clientId,
                                          String clientSecret,
                                          String scope,
                                          long refreshLeewaySeconds) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required");
        }
        if (clientSecret == null) {
            throw new IllegalArgumentException("clientSecret is required");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.refreshLeewaySeconds = refreshLeewaySeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * @return a valid, non-expired bearer token. Refreshes synchronously
     *         when the cached one is missing or within the configured
     *         expiry leeway.
     * @throws ApiException when the authorization server rejects the
     *         credentials or the response cannot be parsed.
     */
    public String getAccessToken() throws ApiException {
        Instant now = Instant.now();
        if (isCachedFresh(now)) {
            return cachedAccessToken;
        }
        synchronized (lock) {
            if (isCachedFresh(Instant.now())) {
                return cachedAccessToken;
            }
            TokenResponse response = fetchNewToken();
            cachedAccessToken = response.accessToken;
            cachedExpiresAt = Instant.now().plusSeconds(Math.max(0, response.expiresIn));
            return cachedAccessToken;
        }
    }

    /**
     * Force a fresh fetch on the next {@link #getAccessToken()} call. Useful
     * when the server has rejected a previously-cached token (e.g. after a
     * client-secret rotation).
     */
    public void invalidate() {
        synchronized (lock) {
            cachedAccessToken = null;
            cachedExpiresAt = null;
        }
    }

    public String getClientId() {
        return clientId;
    }

    private boolean isCachedFresh(Instant now) {
        return cachedAccessToken != null
                && cachedExpiresAt != null
                && now.isBefore(cachedExpiresAt.minusSeconds(refreshLeewaySeconds));
    }

    private TokenResponse fetchNewToken() throws ApiException {
        String body = "grant_type=client_credentials"
                + (scope != null && !scope.isBlank()
                        ? "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8)
                        : "");
        String basic = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + TOKEN_ENDPOINT_PATH))
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return objectMapper.readValue(response.body(), TokenResponse.class);
            }
            // Authorization server returns an OAuth2 error JSON; surface it as
            // a rich ApiException so callers can branch on it just like any
            // other API failure.
            ProblemDetail pd = parseProblemDetail(response.body());
            throw new ApiException(status, pd != null ? pd
                    : problemDetailFromOAuth2Error(status, response.body()));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException(e);
        }
    }

    private ProblemDetail parseProblemDetail(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, ProblemDetail.class);
        } catch (IOException ex) {
            return null;
        }
    }

    private static ProblemDetail problemDetailFromOAuth2Error(int status, String body) {
        ProblemDetail pd = new ProblemDetail();
        pd.setStatus(status);
        pd.setTitle("Failed to obtain client_credentials access token: " + body);
        return pd;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenResponse {
        @JsonProperty("access_token")
        public String accessToken;
        @JsonProperty("token_type")
        public String tokenType;
        @JsonProperty("expires_in")
        public long expiresIn;
        @JsonProperty("scope")
        public String scope;
    }
}
