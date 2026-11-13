package cv.igrp.framework.auth.autentika.client;

import cv.igrp.framework.auth.autentika.config.AutentikaConfiguration;
import cv.igrp.framework.auth.core.exception.IAMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AutentikaTokenProviderTest {
    private static final URI TOKEN_ENDPOINT = URI.create("https://gateway.example.test/token");
    private static final URI REGISTRATION_ENDPOINT = URI.create("https://gateway.example.test/register");
    private HttpClient http;
    private Clock clock;
    private AutentikaTokenProvider provider;
    private final Instant now = Instant.parse("2026-11-13T12:00:00Z");

    @BeforeEach
    void setUp() {
        http = mock(HttpClient.class, withSettings().mockMaker("mock-maker-subclass"));
        clock = mock(Clock.class, withSettings().mockMaker("mock-maker-subclass"));
        when(clock.instant()).thenReturn(now);
        provider = new AutentikaTokenProvider(TOKEN_ENDPOINT, "consumer:key", "consumer+secret",
                Duration.ofSeconds(10), http, clock);
    }

    @Test
    void tokenRequestUsesConsumerCredentialsAndCachesResult() throws Exception {
        reply(200, token("first", 100));
        assertEquals("first", provider.get());
        assertEquals("first", provider.get());
        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(http, times(1)).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("POST", request.getValue().method());
        assertEquals(TOKEN_ENDPOINT + "?grant_type=client_credentials", request.getValue().uri().toString());
        String basic = request.getValue().headers().firstValue("Authorization").orElseThrow();
        assertEquals("consumer%3Akey:consumer%2Bsecret",
                new String(Base64.getDecoder().decode(basic.substring(6)), StandardCharsets.UTF_8));
    }

    @Test
    void renewsBeforeExpiryAndNeverReturnsExpiredTokenAfterFailure() throws Exception {
        reply(200, token("first", 100));
        provider.get();
        when(clock.instant()).thenReturn(now.plusSeconds(90));
        reply(200, token("second", 100));
        assertEquals("second", provider.get());
        when(clock.instant()).thenReturn(now.plusSeconds(200));
        reply(401, "private-response-body");
        var failure = assertThrows(IllegalStateException.class, provider::get);
        assertTrue(failure.getMessage().contains("401"));
        assertFalse(failure.getMessage().contains("private-response-body"));
        verify(http, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void invalidTokenResponseIsNotCachedOrExposed() throws Exception {
        for (String body : new String[]{"not-json-private-secret", "{}", token("", 100), token("secret", 0)}) {
            reply(200, body);
            var failure = assertThrows(IllegalStateException.class, provider::get);
            assertNull(failure.getCause());
            assertFalse(failure.getMessage().contains(body));
        }
        reply(200, token("valid", 100));
        assertEquals("valid", provider.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interruptionPreservesInterruptFlag() throws Exception {
        doThrow(new InterruptedException()).when(http).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        try {
            assertThrows(IllegalStateException.class, provider::get);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void libraryUsesIssuedTokenForManagementRequest() throws Exception {
        reply(200, token("gateway-token", 100));
        HttpClient management = mock(HttpClient.class, withSettings().mockMaker("mock-maker-subclass"));
        HttpResponse<String> response = response(204, "");
        doReturn(response).when(management).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        var client = new AutentikaClient(new AutentikaConfiguration(REGISTRATION_ENDPOINT,
                TOKEN_ENDPOINT, provider, Duration.ofSeconds(10)), management);
        client.deleteClient("id");
        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(management).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("Bearer gateway-token", request.getValue().headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    void tokenFailureIsReportedAsIamExceptionWithoutManagementRequest() throws Exception {
        reply(403, "{}");
        HttpClient management = mock(HttpClient.class, withSettings().mockMaker("mock-maker-subclass"));
        var client = new AutentikaClient(new AutentikaConfiguration(REGISTRATION_ENDPOINT,
                TOKEN_ENDPOINT, provider, Duration.ofSeconds(10)), management);
        assertThrows(IAMException.class, () -> client.deleteClient("id"));
        verifyNoInteractions(management);
    }

    private void reply(int status, String body) throws Exception {
        doReturn(response(status, body)).when(http).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class, withSettings().mockMaker("mock-maker-subclass"));
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    private static String token(String value, int lifetime) {
        return "{\"access_token\":\"" + value + "\",\"token_type\":\"Bearer\",\"expires_in\":" + lifetime + "}";
    }
}
