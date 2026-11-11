package cv.igrp.framework.auth.keycloak.adapter;

import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.core.model.ClientRegistration;
import cv.igrp.framework.auth.core.model.IssuedClient;
import cv.igrp.framework.auth.keycloak.client.KeycloakClientFactory;
import cv.igrp.framework.auth.keycloak.config.KeycloakProperties;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service clients in the Keycloak realm: confidential clients whose secret Keycloak
 * generates and keeps. {@link KeycloakAdapter} delegates its client operations here.
 */
class KeycloakClients {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakClients.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String IN_KEYCLOAK = " in Keycloak";

    static final String CLIENT_CREDENTIALS = "client_credentials";
    static final String AUTHORIZATION_CODE = "authorization_code";
    static final String REFRESH_TOKEN = "refresh_token";
    static final String PASSWORD = "password";
    private static final Set<String> GRANT_TYPES = Set.of(CLIENT_CREDENTIALS, AUTHORIZATION_CODE, REFRESH_TOKEN, PASSWORD);

    private final KeycloakClientFactory factory;
    private final KeycloakProperties properties;
    private final HttpClient http;
    private final Clock clock;

    KeycloakClients(KeycloakClientFactory factory, KeycloakProperties properties) {
        this(factory, properties, HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), Clock.systemUTC());
    }

    KeycloakClients(KeycloakClientFactory factory, KeycloakProperties properties, HttpClient http, Clock clock) {
        this.factory = factory;
        this.properties = properties;
        this.http = http;
        this.clock = clock;
    }

    IssuedClient create(ClientRegistration registration) throws IAMException {
        String clientId = registration.clientId() == null || registration.clientId().isBlank()
                ? UUID.randomUUID().toString() : registration.clientId();
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setProtocol("openid-connect");
        client.setPublicClient(false);
        client.setClientAuthenticatorType("client-secret");
        apply(registration, client);

        try (Keycloak keycloak = factory.createClient()) {
            ClientsResource clients = clients(keycloak);
            try (Response response = clients.create(client)) {
                if (response.getStatus() == 409) {
                    throw new IAMException("Client id already exists in Keycloak: " + clientId);
                }
                if (response.getStatus() != 201) {
                    throw new IAMException("Keycloak refused the client " + clientId + ", HTTP " + response.getStatus());
                }
            }
            String secret = clients.get(internalId(clients, clientId)).getSecret().getValue();
            LOGGER.info("Service client '{}' created in Keycloak", clientId);
            return new IssuedClient(clientId, secret, null);
        } catch (IAMException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IAMException("Error creating client " + clientId + IN_KEYCLOAK, ex);
        }
    }

    IssuedClient rotateSecret(String clientId) throws IAMException {
        try (Keycloak keycloak = factory.createClient()) {
            ClientsResource clients = clients(keycloak);
            String secret = clients.get(internalId(clients, clientId)).generateNewSecret().getValue();
            LOGGER.info("Secret of service client '{}' rotated in Keycloak", clientId);
            return new IssuedClient(clientId, secret, null);
        } catch (IAMException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IAMException("Error rotating the secret of client " + clientId + IN_KEYCLOAK, ex);
        }
    }

    void update(String clientId, ClientRegistration registration) throws IAMException {
        try (Keycloak keycloak = factory.createClient()) {
            ClientsResource clients = clients(keycloak);
            ClientResource resource = clients.get(internalId(clients, clientId));
            ClientRepresentation client = resource.toRepresentation();
            apply(registration, client);
            resource.update(client);
            LOGGER.info("Service client '{}' updated in Keycloak", clientId);
        } catch (IAMException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IAMException("Error updating client " + clientId + IN_KEYCLOAK, ex);
        }
    }

    void delete(String clientId) throws IAMException {
        try (Keycloak keycloak = factory.createClient()) {
            ClientsResource clients = clients(keycloak);
            List<ClientRepresentation> found = clients.findByClientId(clientId);
            if (found.isEmpty()) {
                LOGGER.warn("Service client '{}' is not in Keycloak; nothing to delete", clientId);
                return;
            }
            clients.get(found.getFirst().getId()).remove();
            LOGGER.info("Service client '{}' deleted from Keycloak", clientId);
        } catch (RuntimeException ex) {
            throw new IAMException("Error deleting client " + clientId + " from Keycloak", ex);
        }
    }

    Optional<Instant> authenticate(String clientId, String clientSecret) throws IAMException {
        String form = "grant_type=" + CLIENT_CREDENTIALS
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(tokenEndpoint())
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new IAMException("Keycloak token endpoint unreachable", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IAMException("Interrupted while calling the Keycloak token endpoint", ex);
        }
        if (response.statusCode() == 400 || response.statusCode() == 401) {
            LOGGER.info("Keycloak refused the credentials of client '{}' (HTTP {})", clientId, response.statusCode());
            return Optional.empty();
        }
        if (response.statusCode() != 200) {
            throw new IAMException("Keycloak token endpoint answered HTTP " + response.statusCode());
        }
        return Optional.of(expiryOf(clientId, response.body()));
    }

    /** The token must be Keycloak's answer for this very client; its {@code exp} bounds how long AM trusts it. */
    private Instant expiryOf(String clientId, String body) throws IAMException {
        try {
            String token = JSON.readTree(body).path("access_token").asString("");
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IAMException("Keycloak returned an access token that is not a JWT");
            }
            JsonNode claims = JSON.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!clientId.equals(claims.path("azp").asString(""))) {
                throw new IAMException("Keycloak returned a token for another client");
            }
            Instant exp = Instant.ofEpochSecond(claims.path("exp").asLong(0));
            if (!exp.isAfter(clock.instant())) {
                throw new IAMException("Keycloak returned an expired token");
            }
            return exp;
        } catch (IAMException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IAMException("Keycloak returned an unreadable token response", ex);
        }
    }

    private URI tokenEndpoint() {
        String base = properties.getServerUrl().endsWith("/")
                ? properties.getServerUrl().substring(0, properties.getServerUrl().length() - 1)
                : properties.getServerUrl();
        return URI.create(base + "/realms/" + URLEncoder.encode(properties.getRealm(), StandardCharsets.UTF_8)
                + "/protocol/openid-connect/token");
    }

    private static void apply(ClientRegistration registration, ClientRepresentation client) throws IAMException {
        for (String grant : registration.grantTypes()) {
            if (!GRANT_TYPES.contains(grant)) {
                throw new IAMException("Grant type not supported for Keycloak clients: " + grant);
            }
        }
        Set<String> grants = registration.grantTypes();
        client.setName(registration.clientName());
        client.setServiceAccountsEnabled(grants.contains(CLIENT_CREDENTIALS));
        client.setStandardFlowEnabled(grants.contains(AUTHORIZATION_CODE));
        client.setDirectAccessGrantsEnabled(grants.contains(PASSWORD));
        client.setImplicitFlowEnabled(false);
        client.setRedirectUris(registration.redirectUris());
    }

    private ClientsResource clients(Keycloak keycloak) {
        return keycloak.realm(properties.getRealm()).clients();
    }

    private static String internalId(ClientsResource clients, String clientId) throws IAMException {
        List<ClientRepresentation> found = clients.findByClientId(clientId);
        if (found.isEmpty()) {
            throw new IAMException("Client not found in Keycloak: " + clientId);
        }
        return found.getFirst().getId();
    }
}
