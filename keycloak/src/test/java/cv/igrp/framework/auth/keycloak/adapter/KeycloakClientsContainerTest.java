package cv.igrp.framework.auth.keycloak.adapter;

import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.core.model.ClientRegistration;
import cv.igrp.framework.auth.core.model.IssuedClient;
import cv.igrp.framework.auth.keycloak.client.KeycloakClientFactory;
import cv.igrp.framework.auth.keycloak.config.KeycloakProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3-17 (#24): service clients through {@link KeycloakAdapter}, against a real Keycloak.
 * AM's adapter account ({@code igrp-admin}) holds only realm-management's client roles.
 */
class KeycloakClientsContainerTest {

    @SuppressWarnings("resource")
    static final GenericContainer<?> KEYCLOAK =
            new GenericContainer<>(DockerImageName.parse("quay.io/keycloak/keycloak:26.0.7"))
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                    .withCommand("start-dev")
                    .withExposedPorts(8080)
                    .waitingFor(Wait.forHttp("/realms/master").forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    static final KeycloakProperties PROPERTIES = new KeycloakProperties();
    static final KeycloakAdapter ADAPTER;

    static {
        KEYCLOAK.start();
        String serverUrl = "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);
        createRealmWithAdapterAccount(serverUrl);
        PROPERTIES.setServerUrl(serverUrl);
        PROPERTIES.setRealm("igrp-test");
        PROPERTIES.setClientId("igrp-admin");
        PROPERTIES.setClientSecret("admin-secret");
        PROPERTIES.setGrantType("client_credentials");
        ADAPTER = new KeycloakAdapter(new KeycloakClientFactory(PROPERTIES), PROPERTIES);
    }

    /** Realm {@code igrp-test} with {@code igrp-admin}: a service account holding only realm-management's client roles. */
    private static void createRealmWithAdapterAccount(String serverUrl) {
        try (Keycloak master = KeycloakBuilder.builder().serverUrl(serverUrl).realm("master")
                .clientId("admin-cli").username("admin").password("admin").grantType("password").build()) {
            RealmRepresentation realm = new RealmRepresentation();
            realm.setRealm("igrp-test");
            realm.setEnabled(true);
            master.realms().create(realm);

            ClientRepresentation admin = new ClientRepresentation();
            admin.setClientId("igrp-admin");
            admin.setPublicClient(false);
            admin.setSecret("admin-secret");
            admin.setServiceAccountsEnabled(true);
            admin.setStandardFlowEnabled(false);
            RealmResource igrp = master.realm("igrp-test");
            igrp.clients().create(admin).close();

            String adminId = igrp.clients().findByClientId("igrp-admin").getFirst().getId();
            String serviceAccountId = igrp.clients().get(adminId).getServiceAccountUser().getId();
            String realmManagementId = igrp.clients().findByClientId("realm-management").getFirst().getId();
            List<RoleRepresentation> roles = Stream.of("manage-clients", "view-clients", "query-clients")
                    .map(role -> igrp.clients().get(realmManagementId).roles().get(role).toRepresentation())
                    .toList();
            igrp.users().get(serviceAccountId).roles().clientLevel(realmManagementId).add(roles);
        }
    }

    private static ClientRegistration service(String clientId) {
        return new ClientRegistration(clientId, "Faturação API", Set.of("client_credentials"), List.of());
    }

    private static ClientRepresentation inKeycloak(String clientId) {
        try (Keycloak keycloak = new KeycloakClientFactory(PROPERTIES).createClient()) {
            List<ClientRepresentation> found = keycloak.realm("igrp-test").clients().findByClientId(clientId);
            return found.isEmpty() ? null : found.getFirst();
        }
    }

    @Test
    @DisplayName("Create returns the IdP's secret, and those credentials get a token from Keycloak")
    void createAndAuthenticate() throws IAMException {
        String clientId = "faturacao-api-" + UUID.randomUUID();
        IssuedClient issued = ADAPTER.createClient(service(clientId));

        assertEquals(clientId, issued.clientId());
        assertFalse(issued.clientSecret().isBlank());
        assertNull(issued.secretExpiresAt());

        ClientRepresentation client = inKeycloak(clientId);
        assertEquals("Faturação API", client.getName());
        assertTrue(client.isServiceAccountsEnabled());
        assertFalse(client.isStandardFlowEnabled());
        assertFalse(client.isPublicClient());

        Optional<Instant> expiry = ADAPTER.authenticateClient(clientId, issued.clientSecret());
        assertTrue(expiry.orElseThrow().isAfter(Instant.now()));
        assertTrue(ADAPTER.authenticateClient(clientId, "wrong-secret").isEmpty());
        assertTrue(ADAPTER.authenticateClient("no-such-client", "x").isEmpty());
    }

    @Test
    @DisplayName("Without a client id, the IdP's client gets a generated one")
    void generatedClientId() throws IAMException {
        IssuedClient issued = ADAPTER.createClient(service(null));
        assertFalse(issued.clientId().isBlank());
        assertEquals("Faturação API", inKeycloak(issued.clientId()).getName());
    }

    @Test
    @DisplayName("A client id that already exists is refused")
    void duplicateClientId() throws IAMException {
        String clientId = "dup-" + UUID.randomUUID();
        ADAPTER.createClient(service(clientId));
        IAMException failure = assertThrows(IAMException.class, () -> ADAPTER.createClient(service(clientId)));
        assertTrue(failure.getMessage().contains("already exists"), failure.getMessage());
    }

    @Test
    @DisplayName("Rotate: the new secret works and the old one stops working")
    void rotate() throws IAMException {
        String clientId = "rotate-" + UUID.randomUUID();
        String oldSecret = ADAPTER.createClient(service(clientId)).clientSecret();

        String newSecret = ADAPTER.rotateClientSecret(clientId).clientSecret();

        assertNotEquals(oldSecret, newSecret);
        assertTrue(ADAPTER.authenticateClient(clientId, newSecret).isPresent());
        assertTrue(ADAPTER.authenticateClient(clientId, oldSecret).isEmpty());
    }

    @Test
    @DisplayName("Update changes name, grant types and redirect URIs, never the client id")
    void update() throws IAMException {
        String clientId = "update-" + UUID.randomUUID();
        ADAPTER.createClient(service(clientId));

        ADAPTER.updateClient(clientId, new ClientRegistration("ignored", "Faturação API v2",
                Set.of("authorization_code", "refresh_token"), List.of("https://faturacao.example/cb")));

        ClientRepresentation client = inKeycloak(clientId);
        assertEquals("Faturação API v2", client.getName());
        assertFalse(client.isServiceAccountsEnabled());
        assertTrue(client.isStandardFlowEnabled());
        assertEquals(List.of("https://faturacao.example/cb"), client.getRedirectUris());
        assertNull(inKeycloak("ignored"));
    }

    @Test
    @DisplayName("Delete removes the client; deleting it again is not an error")
    void delete() throws IAMException {
        String clientId = "delete-" + UUID.randomUUID();
        String secret = ADAPTER.createClient(service(clientId)).clientSecret();

        ADAPTER.deleteClient(clientId);
        ADAPTER.deleteClient(clientId);

        assertNull(inKeycloak(clientId));
        assertTrue(ADAPTER.authenticateClient(clientId, secret).isEmpty());
    }

    @Test
    @DisplayName("Rotate and update of an unknown client fail with the client id")
    void unknownClient() {
        IAMException rotate = assertThrows(IAMException.class, () -> ADAPTER.rotateClientSecret("ghost"));
        assertTrue(rotate.getMessage().contains("ghost"), rotate.getMessage());
        ClientRegistration registration = service(null);
        assertThrows(IAMException.class, () -> ADAPTER.updateClient("ghost", registration));
    }
}
