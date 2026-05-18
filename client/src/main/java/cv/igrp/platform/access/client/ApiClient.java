package cv.igrp.platform.access.client;

import cv.igrp.platform.access.client.auth.ClientCredentialsTokenProvider;
import cv.igrp.platform.access.client.errors.ProblemDetail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class ApiClient implements IApiClient {
    private final HttpClient httpClient;

    // [marcelo.monteiro - 2025-06-26 11:52:04] TODO: Get these default data from properties
    private String baseUrl = "http://localhost:8081"; // TODO: read from properties
    private String authToken;
    private ClientCredentialsTokenProvider clientCredentialsTokenProvider;
    private final int timeout = 30;

    public ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
    }

    @Override
    public String getToken() {
        return authToken;
    }

    @Override
    public <T> T invokeAPI(String path, String method, Map<String, String> queryParams, Object body,
                           Map<String, String> headerParams, Class<T> returnType) throws ApiException {
        try {
            HttpResponse<String> response = getStringHttpResponse(path, method, queryParams, body, headerParams);
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return JsonUtil.deserialize(response.body(), returnType);
            } else {
                throw buildApiException(response);
            }
        } catch (IOException | InterruptedException e) {
            throw new ApiException(e);
        }
    }

    @Override
    public String invokeAPIRaw(String path, String method, Map<String, String> queryParams, Object body,
                               Map<String, String> headerParams) throws ApiException, IOException, InterruptedException {
        HttpResponse<String> response = getStringHttpResponse(path, method, queryParams, body, headerParams);
        int statusCode = response.statusCode();
        if (statusCode >= 200 && statusCode < 300) {
            return response.body(); // Already string
        } else {
            throw buildApiException(response);
        }
    }

    /**
     * Parse the response body as an RFC-7807 {@link ProblemDetail} (the shape
     * the Access Management API's {@code GlobalExceptionHandler} returns on
     * every error) and wrap the result in a rich {@link ApiException}.
     * Falls back to the legacy "raw body in message" shape when the response
     * is not parseable (e.g. plain-text 502 from a reverse proxy ahead of
     * the API).
     */
    private static ApiException buildApiException(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String body = response.body();
        if (body == null || body.isBlank()) {
            return new ApiException(statusCode, "API Error: status=" + statusCode);
        }
        // The API always returns problem+json or application/json on error.
        // Anything else (plain text, html) → legacy fallback.
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        boolean looksJson = contentType.contains("json") || body.trim().startsWith("{");
        if (looksJson) {
            try {
                ProblemDetail pd = JsonUtil.deserialize(body, ProblemDetail.class);
                if (pd != null) {
                    return new ApiException(statusCode, pd);
                }
            } catch (Exception ignored) {
                // Fall through to legacy shape (any parse / Jackson failure).
            }
        }
        return new ApiException(statusCode, "API Error: " + body);
    }

    private HttpResponse<String> getStringHttpResponse(String path, String method, Map<String, String> queryParams,
                                                       Object body, Map<String, String> headerParams)
            throws IOException, InterruptedException, ApiException {
        StringBuilder uriBuilder = new StringBuilder(baseUrl + path);
        if (queryParams != null && !queryParams.isEmpty()) {
            uriBuilder.append("?");
            queryParams.forEach((k, v) -> uriBuilder.append(k).append("=").append(v).append("&"));
            uriBuilder.deleteCharAt(uriBuilder.length() - 1);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(uriBuilder.toString()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        if (authToken != null) {
            builder.header("Authorization", "Bearer " + authToken);
        }

        if (headerParams != null) {
            headerParams.forEach(builder::header);
        }

        String requestBody = body != null ? JsonUtil.serialize(body) : null;
        switch (method) {
            case "GET":
                builder.GET();
                break;
            case "POST":
                builder.POST(requestBody != null
                        ? HttpRequest.BodyPublishers.ofString(requestBody)
                        : HttpRequest.BodyPublishers.noBody());
                break;
            case "PUT":
                builder.PUT(requestBody != null
                        ? HttpRequest.BodyPublishers.ofString(requestBody)
                        : HttpRequest.BodyPublishers.noBody());
                break;
            case "DELETE":
                builder.DELETE();
                break;
            default:
                throw new ApiException(415, "Unsupported method: " + method);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    /**
     * Configure OAuth2 {@code client_credentials} authentication. After this
     * call, every API invocation that asks for an automatic Bearer (e.g.
     * the M2M sync endpoints via {@code M2MApi}) will fetch and cache a
     * token from {@code {baseUrl}/oauth2/token} using the supplied
     * client credentials. The token is reused across calls and refreshed
     * transparently before expiry.
     *
     * <p>Centralises what used to be a per-call static M2M header — callers
     * now provide their (clientId, clientSecret) once and the rest is
     * handled by the client library.
     */
    public void setClientCredentials(String clientId, String clientSecret) {
        this.clientCredentialsTokenProvider = new ClientCredentialsTokenProvider(
                baseUrl, clientId, clientSecret);
    }

    /**
     * Variant accepting a scope (e.g. {@code "m2m"}) when the OAuth2 client
     * registration constrains scopes.
     */
    public void setClientCredentials(String clientId, String clientSecret, String scope) {
        this.clientCredentialsTokenProvider = new ClientCredentialsTokenProvider(
                baseUrl, clientId, clientSecret, scope, 30);
    }

    /**
     * @return the configured token provider, or {@code null} when no client
     *         credentials have been set. M2M-aware API classes use this to
     *         decide whether to inject {@code Authorization: Bearer} on
     *         their calls.
     */
    public ClientCredentialsTokenProvider getClientCredentialsTokenProvider() {
        return clientCredentialsTokenProvider;
    }
}
