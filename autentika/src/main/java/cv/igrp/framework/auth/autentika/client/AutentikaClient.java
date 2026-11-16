package cv.igrp.framework.auth.autentika.client;

import cv.igrp.framework.auth.autentika.config.AutentikaConfiguration;
import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.core.model.ClientRegistration;
import cv.igrp.framework.auth.core.model.IssuedClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.Set;

/** Autentika gateway client for the OAuth DCR v1.1 contract shipped with Identity Server 5.11. */
public final class AutentikaClient {

    private static final String CLIENT_NAME = "client_name";
    private static final String CLIENT_ID = "client_id";

    private final AutentikaConfiguration configuration;
    private final HttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public AutentikaClient(AutentikaConfiguration configuration) {
        this(configuration, HttpClient.newBuilder().connectTimeout(configuration.requestTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    public AutentikaClient(AutentikaConfiguration configuration, HttpClient http) {
        this.configuration = Objects.requireNonNull(configuration);
        this.http = Objects.requireNonNull(http);
    }

    /**
     * IAdapter's name-only convenience operation creates a machine-to-machine client.
     * Use createClient when credentials, grant types or redirect URIs are needed.
     */
    public void createApplication(String name) throws IAMException {
        createClient(new ClientRegistration(null, required(name), Set.of("client_credentials"), List.of()));
    }

    public void updateApplication(String name, String newName) throws IAMException {
        JsonNode application = findApplicationByName(name);
        Map<String, Object> update = new LinkedHashMap<>();
        update.put(CLIENT_NAME, required(newName));
        // Preserve the existing configuration; never copy credentials into an update.
        for (String field : List.of("grant_types", "redirect_uris",
                "backchannel_logout_uri", "backchannel_logout_session_required")) {
            if (application.has(field)) update.put(field, application.get(field));
        }
        admin("PUT", child(configuration.registrationEndpoint(), text(application, CLIENT_ID)), update, false);
    }

    public void deleteApplication(String name) throws IAMException {
        deleteClient(text(findApplicationByName(name), CLIENT_ID));
    }
    public IssuedClient createClient(ClientRegistration registration) throws IAMException {
        Map<String, Object> body = registrationBody(registration);
        if (registration.clientId() != null && !registration.clientId().isBlank()) {
            body.put("ext_param_client_id", registration.clientId());
        }
        JsonNode response = admin("POST", configuration.registrationEndpoint(), body, false);
        return new IssuedClient(text(response, CLIENT_ID), text(response, "client_secret"), secretExpiry(response));
    }

    public void updateClient(String clientId, ClientRegistration registration) throws IAMException {
        admin("PUT", child(configuration.registrationEndpoint(), clientId), registrationBody(registration), false);
    }

    public void deleteClient(String clientId) throws IAMException {
        admin("DELETE", child(configuration.registrationEndpoint(), clientId), null, true);
    }

    public IssuedClient rotateClientSecret() {
        throw new UnsupportedOperationException(
                "Autentika DCR v1.1 does not expose a secret-rotation operation");
    }

    public Optional<Instant> authenticateClient(String clientId, String clientSecret) throws IAMException {
        required(clientId);
        required(clientSecret);
        Instant requestedAt = Instant.now();
        HttpResponse<String> response = send("POST", configuration.tokenEndpoint(),
                basic(encode(clientId), encode(clientSecret)), "application/x-www-form-urlencoded",
                "grant_type=client_credentials");
        if (response.statusCode() == 400 || response.statusCode() == 401) {
            JsonNode error = parse(response.body());
            if ("invalid_client".equals(error.path("error").asString())) {
                return Optional.empty();
            }
        }
        requireSuccess(response);
        JsonNode token = parse(response.body());
        text(token, "access_token");
        JsonNode expiresIn = token.path("expires_in");
        try {
            long seconds = Long.parseLong(expiresIn.asString());
            if (seconds <= 0) throw new IllegalArgumentException();
            return Optional.of(requestedAt.plusSeconds(seconds));
        } catch (RuntimeException e) {
            throw new IAMException("Autentika token response has invalid expires_in");
        }
    }

    private JsonNode findApplicationByName(String name) throws IAMException {
        URI endpoint = URI.create(configuration.registrationEndpoint().toString().replaceAll("/$", "")
                + "?client_name=" + encode(required(name)));
        JsonNode application = admin("GET", endpoint, null, false);
        if (!name.equals(text(application, CLIENT_NAME))) {
            throw new IAMException("Autentika lookup returned a different application name");
        }
        return application;
    }

    private Map<String, Object> registrationBody(ClientRegistration registration) {
        Objects.requireNonNull(registration, "registration");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(CLIENT_NAME, required(registration.clientName()));
        body.put("grant_types", registration.grantTypes());
        body.put("redirect_uris", registration.redirectUris());
        return body;
    }

    private JsonNode admin(String method, URI endpoint, Object body, boolean missingIsSuccess) throws IAMException {
        String payload;
        try {
            payload = body == null ? "" : json.writeValueAsString(body);
        } catch (RuntimeException e) {
            throw new IAMException("Could not serialize Autentika request");
        }
        HttpResponse<String> response = send(method, endpoint,
                gatewayAuthorization(), "application/json", payload);
        if (missingIsSuccess && response.statusCode() == 404) {
            // A gateway can return 404 for an unpublished route, not a missing client.
            // Only a DCR invalid_client response confirms that the client is absent.
            JsonNode error;
            try {
                error = parse(response.body());
            } catch (IAMException e) {
                throw new IAMException("Autentika returned HTTP 404; client deletion could not be confirmed");
            }
            if ("invalid_client".equals(error.path("error").asString())) return json.createObjectNode();
            throw new IAMException("Autentika returned HTTP 404; client deletion could not be confirmed");
        }
        requireSuccess(response);
        return response.body().isBlank() ? json.createObjectNode() : parse(response.body());
    }

    private String gatewayAuthorization() throws IAMException {
        String token;
        try {
            token = configuration.gatewayAccessToken().get();
        } catch (RuntimeException e) {
            throw new IAMException("Could not obtain an Autentika gateway access token", e);
        }
        if (token == null || token.isBlank() || token.contains("\r") || token.contains("\n")) {
            throw new IAMException("An Autentika gateway access token is required");
        }
        return "Bearer " + token;
    }

    private HttpResponse<String> send(String method, URI endpoint, String authorization,
                                      String contentType, String body) throws IAMException {
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(configuration.requestTimeout())
                .header("Authorization", authorization).header("Accept", "application/json")
                .header("Content-Type", contentType)
                .method(method, body.isEmpty() ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IAMException("Autentika request interrupted", e);
        } catch (IOException e) {
            throw new IAMException("Could not communicate with Autentika", e);
        }
    }

    private void requireSuccess(HttpResponse<String> response) throws IAMException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            // Never include response bodies: they may contain credentials or tokens.
            throw new IAMException("Autentika request failed with HTTP " + response.statusCode());
        }
    }

    private JsonNode parse(String body) throws IAMException {
        try {
            JsonNode parsed = json.readTree(body);
            if (parsed == null || !parsed.isObject()) throw new IllegalArgumentException();
            return parsed;
        } catch (RuntimeException e) {
            throw new IAMException("Autentika returned an invalid JSON object");
        }
    }

    private static String text(JsonNode node, String field) throws IAMException {
        JsonNode value = node.path(field);
        if (!value.isString() || value.asString().isBlank()) {
            throw new IAMException("Autentika response is missing " + field);
        }
        return value.asString();
    }

    private static Instant secretExpiry(JsonNode response) throws IAMException {
        JsonNode expiry = response.path("client_secret_expires_at");
        if (expiry.isMissingNode() || expiry.isNull()) return null;
        try {
            long seconds = Long.parseLong(expiry.asString());
            if (seconds == 0) return null;
            if (seconds < 0) throw new IllegalArgumentException();
            return Instant.ofEpochSecond(seconds);
        } catch (RuntimeException e) {
            throw new IAMException("Autentika response has invalid client_secret_expires_at");
        }
    }

    private static URI child(URI base, String id) {
        return URI.create(base.toString().replaceAll("/$", "") + "/" + encode(required(id)).replace("+", "%20"));
    }

    private static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("A nonblank value is required");
        return value;
    }
}
