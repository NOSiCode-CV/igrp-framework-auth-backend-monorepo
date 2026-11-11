package cv.igrp.framework.auth.keycloak.adapter;

import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.core.model.ClientRegistration;
import cv.igrp.framework.auth.keycloak.client.KeycloakClientFactory;
import cv.igrp.framework.auth.keycloak.config.KeycloakProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * v3-17 (#24): how {@link KeycloakClients} reads the token endpoint's answers, case by case.
 * The happy paths run against a real Keycloak in {@code KeycloakClientsContainerTest}.
 */
class KeycloakClientsTest {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    private final HttpClient http = mock(HttpClient.class);
    private final KeycloakClientFactory factory = mock(KeycloakClientFactory.class);
    private final KeycloakClients clients = new KeycloakClients(factory, properties(), http, Clock.fixed(NOW, ZoneOffset.UTC));

    private static KeycloakProperties properties() {
        KeycloakProperties properties = new KeycloakProperties();
        properties.setServerUrl("http://keycloak.test/");
        properties.setRealm("igrp");
        return properties;
    }

    @SuppressWarnings("unchecked")
    private void tokenEndpointAnswers(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn((HttpResponse) response);
    }

    private static String token(String azp, Instant exp) {
        String claims = "{\"azp\":\"" + azp + "\",\"exp\":" + exp.getEpochSecond() + "}";
        return "{\"access_token\":\"h." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(claims.getBytes(StandardCharsets.UTF_8)) + ".s\",\"expires_in\":300}";
    }

    @Test
    @DisplayName("Accepted credentials: the token's exp, from a client_credentials POST to the realm's token endpoint")
    void accepted() throws Exception {
        Instant exp = NOW.plusSeconds(300);
        tokenEndpointAnswers(200, token("svc a", exp));

        assertEquals(exp, clients.authenticate("svc a", "s&cret").orElseThrow());

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(request.capture(), any());
        assertEquals("http://keycloak.test/realms/igrp/protocol/openid-connect/token", request.getValue().uri().toString());
        assertEquals("POST", request.getValue().method());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401})
    @DisplayName("Refused credentials are an empty answer, not an error")
    void refused(int status) throws Exception {
        tokenEndpointAnswers(status, "{\"error\":\"unauthorized_client\"}");
        assertTrue(clients.authenticate("svc", "wrong").isEmpty());
    }

    @Test
    @DisplayName("Any other status is an IdP failure")
    void idpFailure() throws Exception {
        tokenEndpointAnswers(503, "");
        IAMException failure = assertThrows(IAMException.class, () -> clients.authenticate("svc", "s"));
        assertTrue(failure.getMessage().contains("503"), failure.getMessage());
    }

    @Test
    @DisplayName("A token issued for another client is rejected")
    void otherClient() throws Exception {
        tokenEndpointAnswers(200, token("someone-else", NOW.plusSeconds(300)));
        assertThrows(IAMException.class, () -> clients.authenticate("svc", "s"));
    }

    @Test
    @DisplayName("An already expired token is rejected")
    void expired() throws Exception {
        tokenEndpointAnswers(200, token("svc", NOW));
        assertThrows(IAMException.class, () -> clients.authenticate("svc", "s"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"access_token\":\"opaque\"}", "{}", "not json", "{\"access_token\":\"a.!!!.c\"}"})
    @DisplayName("A response without a readable JWT is an IdP failure")
    void unreadable(String body) throws Exception {
        tokenEndpointAnswers(200, body);
        assertThrows(IAMException.class, () -> clients.authenticate("svc", "s"));
    }

    @Test
    @DisplayName("An unreachable token endpoint is an IdP failure")
    void unreachable() throws Exception {
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("refused"));
        assertThrows(IAMException.class, () -> clients.authenticate("svc", "s"));
    }

    @Test
    @DisplayName("An interrupted call keeps the thread's interrupt flag")
    void interrupted() throws Exception {
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException());
        try {
            assertThrows(IAMException.class, () -> clients.authenticate("svc", "s"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("A grant type Keycloak clients cannot have is refused before calling Keycloak")
    void unsupportedGrant() {
        ClientRegistration registration = new ClientRegistration("svc", "Svc", Set.of("implicit"), List.of());
        IAMException failure = assertThrows(IAMException.class, () -> clients.create(registration));
        assertTrue(failure.getMessage().contains("implicit"), failure.getMessage());
        verifyNoInteractions(factory);
    }
}
