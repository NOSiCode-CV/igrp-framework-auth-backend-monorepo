package cv.igrp.platform.access.client;

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
                throw new ApiException(statusCode, "API Error: " + response.body());
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
            throw new ApiException(statusCode, "API Error: " + response.body());
        }
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
}
