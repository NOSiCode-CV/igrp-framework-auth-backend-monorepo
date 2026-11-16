package cv.igrp.framework.auth.autentika.adapter;

import cv.igrp.framework.auth.autentika.config.AutentikaConfiguration;
import cv.igrp.framework.auth.core.model.ClientRegistration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live tests against an existing Autentika deployment; no mocks or local servers.
 * Run with: mvn -pl autentika -am -Pautentika-integration verify
 * Credentials are required through environment variables; endpoint defaults can be overridden:
 * AUTENTIKA_REGISTRATION_ENDPOINT, AUTENTIKA_TOKEN_ENDPOINT,
 * AUTENTIKA_CONSUMER_KEY, AUTENTIKA_CONSUMER_SECRET.
 * The configured account must manage disposable applications and OAuth clients.
 * Tests create uniquely named resources and delete only resources they created.
 * Missing configuration fails the integration run instead of silently skipping it.
 */
class AutentikaAdapterIntegrationTest {

    // Staging endpoint defaults; credentials must come from environment variables.
    private static final String REGISTRATION_ENDPOINT = "https://gateway-stage-pdex.gov.cv/identity/register/1.0.0/register";
    private static final String TOKEN_ENDPOINT = "https://gateway-stage-pdex.gov.cv/token";

    private static AutentikaAdapter adapter;
    private static AutentikaConfiguration configuration;

    @BeforeAll
    static void configureRealProvider() {
        configuration = AutentikaConfiguration.withClientCredentials(
                URI.create(configuredValue("AUTENTIKA_REGISTRATION_ENDPOINT", REGISTRATION_ENDPOINT)),
                URI.create(configuredValue("AUTENTIKA_TOKEN_ENDPOINT", TOKEN_ENDPOINT)),
                configuredValue("AUTENTIKA_CONSUMER_KEY", null),
                configuredValue("AUTENTIKA_CONSUMER_SECRET", null),
                Duration.ofSeconds(30));
        adapter = new AutentikaAdapter(configuration);
    }
    @Test
    void gatewayTokenCanBeIssued() {
        String gatewayToken = configuration.gatewayAccessToken().get();
        assertNotNull(gatewayToken);
        assertFalse(gatewayToken.isBlank());
    }

    @Test
    void applicationCanBeCreatedRenamedAndDeleted() throws Exception {
        String originalName = "igrp-it-app-" + UUID.randomUUID();
        String renamed = originalName + "-renamed";
        adapter.createApplication(null, originalName);
        String cleanupName = originalName;
        try {
            assertDoesNotThrow(() -> adapter.updateApplication(null, originalName, renamed));
            cleanupName = renamed;
            // A second rename resolves the new name remotely, verifying the first update persisted.
            assertDoesNotThrow(() -> adapter.updateApplication(null, renamed, originalName),
                    "The renamed application must be resolvable by its new name");
            cleanupName = originalName;
        } finally {
            adapter.deleteApplication(null, cleanupName);
        }
    }

    @Test
    void clientCanAuthenticateUpdateAndBeDeleted() throws Exception {
        String name = "igrp-it-client-" + UUID.randomUUID();
        var issued = adapter.createClient(registration(name));
        assertNotNull(issued);
        System.out.println("Created integration-test client: " + name + " (client_id=" + issued.clientId() + ")");
        Throwable failure = null;
        try {
            assertFalse(issued.clientId().isBlank());
            assertFalse(issued.clientSecret().isBlank());
            assertTrue(adapter.authenticateClient(issued.clientId(), issued.clientSecret())
                    .orElseThrow().isAfter(Instant.now()));

            adapter.updateClient(issued.clientId(), registration(name + "-renamed"));
            assertTrue(adapter.authenticateClient(issued.clientId(), issued.clientSecret()).isPresent());

        } catch (Exception | AssertionError e) {
            failure = e;
            throw e;
        } finally {
            try {
                adapter.deleteClient(issued.clientId());
            } catch (Exception cleanupFailure) {
                System.err.println("Manual cleanup may be required for test client_id=" + issued.clientId());
                if (failure != null) failure.addSuppressed(cleanupFailure);
                else throw cleanupFailure;
            }
        }
        assertTrue(adapter.authenticateClient(issued.clientId(), issued.clientSecret()).isEmpty());
        // IAdapter requires deletion of a missing client to succeed.
        adapter.deleteClient(issued.clientId());
    }

    private static ClientRegistration registration(String name) {
        return new ClientRegistration(null, name, Set.of("client_credentials", "authorization_code", "refresh_token"),
                List.of("http://wso2is.local:8081/playground2/oauth2client"));
    }

    private static String configuredValue(String name, String temporaryValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = temporaryValue;
        }
        if (value == null || value.isBlank() || value.startsWith("TODO_")) {
            throw new IllegalStateException("Configure " + name
                    + " via environment before running live tests");
        }
        return value;
    }
}
