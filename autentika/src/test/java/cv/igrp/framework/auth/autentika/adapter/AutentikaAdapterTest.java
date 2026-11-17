package cv.igrp.framework.auth.autentika.adapter;


import cv.igrp.framework.auth.autentika.config.AutentikaConfiguration;
import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.core.model.ClientRegistration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutentikaAdapterTest {
    private static final URI REGISTRATION_ENDPOINT = URI.create("https://autentika.example.test/identity/register/1.0.0/register");

	private HttpClient http;
    private AutentikaAdapter adapter;
    private final ConcurrentLinkedQueue<Reply> replies = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Request> requests = new ConcurrentLinkedQueue<>();
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        http = mock(HttpClient.class, withSettings().mockMaker("mock-maker-subclass"));
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            requests.add(new Request(request.method(), request.uri().getRawPath()
                    + (request.uri().getRawQuery() == null ? "" : "?" + request.uri().getRawQuery()),
                    request.headers().firstValue("Authorization").orElseThrow(), body(request)));
            Reply reply = replies.remove();
            HttpResponse<String> response = mock(HttpResponse.class, withSettings().mockMaker("mock-maker-subclass"));
            when(response.statusCode()).thenReturn(reply.status());
            when(response.body()).thenReturn(reply.body());
            return response;
        });
        String base = "https://autentika.example.test";
        adapter = new AutentikaAdapter(new AutentikaConfiguration(

                URI.create(base + "/identity/register/1.0.0/register"),
                URI.create(base + "/oauth2/token"), () -> "test-gateway-token", Duration.ofSeconds(3)), http);
    }

    private static String body(HttpRequest request) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompletableFuture<String> result = new CompletableFuture<>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            public void onError(Throwable error) { result.completeExceptionally(error); }
            public void onComplete() { result.complete(bytes.toString(StandardCharsets.UTF_8)); }
        });
        return result.get(3, TimeUnit.SECONDS);
    }
    @Test
    void registersOAuthClientAndReturnsGeneratedCredentials() throws Exception {
        replies.add(new Reply(201, "{\"client_id\":\"issued-id\",\"client_secret\":\"issued-secret\",\"client_secret_expires_at\":0}"));
        var issued = adapter.createClient(registration("requested-id"));
        assertEquals("issued-id", issued.clientId());
        assertEquals("issued-secret", issued.clientSecret());
        assertNull(issued.secretExpiresAt());
        Request request = requests.remove();
        assertEquals("POST", request.method());
        assertEquals("/identity/register/1.0.0/register", request.uri());
        assertEquals("Bearer test-gateway-token", request.authorization());
        var body = json.readTree(request.body());
        assertEquals("requested-id", body.path("ext_param_client_id").asString());
        assertEquals("service-app", body.path("client_name").asString());
        assertEquals("client_credentials", body.path("grant_types").get(0).asString());
        assertEquals("https://example.test/callback", body.path("redirect_uris").get(0).asString());
    }

    @Test
    void updatesAndDeletesUsingOAuthClientId() throws Exception {
        replies.add(new Reply(200, "{}"));
        replies.add(new Reply(404, "{\"error\":\"invalid_client\"}"));
        adapter.updateClient("original/id", registration("ignored-new-id"));
        adapter.deleteClient("original/id");
        Request update = requests.remove();
        assertEquals("PUT", update.method());
        assertTrue(update.uri().endsWith("/original%2Fid"));
        assertFalse(json.readTree(update.body()).has("ext_param_client_id"));
        assertEquals("DELETE", requests.remove().method());
    }

    @Test
    void applicationMethodsUseDcrAndPreserveExistingOAuthSettings() throws Exception {
        replies.add(new Reply(201, application("service-app", "client-id")));
        replies.add(new Reply(200, application("service-app", "client-id")));
        replies.add(new Reply(200, "{}"));
        replies.add(new Reply(200, application("renamed", "client-id")));
        replies.add(new Reply(204, ""));
        adapter.createApplication("department", "service-app");
        adapter.updateApplication("department", "service-app", "renamed");
        adapter.deleteApplication("department", "renamed");
        Request create = requests.remove();
        assertEquals("POST", create.method());
        assertEquals("/identity/register/1.0.0/register", create.uri());
        assertEquals("client_credentials", json.readTree(create.body()).path("grant_types").get(0).asString());
        assertEquals("/identity/register/1.0.0/register?client_name=service-app", requests.remove().uri());
        Request update = requests.remove();
        assertEquals("PUT", update.method());
        assertEquals("/identity/register/1.0.0/register/client-id", update.uri());
        var body = json.readTree(update.body());
        assertEquals("renamed", body.path("client_name").asString());
        assertEquals("authorization_code", body.path("grant_types").get(0).asString());
        assertEquals("https://example.test/callback", body.path("redirect_uris").get(0).asString());
        assertFalse(body.has("client_secret"));
        requests.remove();
        assertEquals("/identity/register/1.0.0/register/client-id", requests.remove().uri());
        assertTrue(requests.isEmpty());
    }

    @Test
    void secretRotationIsExplicitlyUnsupportedWithoutHttpCalls() {
        assertThrows(UnsupportedOperationException.class, () -> adapter.rotateClientSecret("client-id"));
        verifyNoInteractions(http);
    }

    @Test
    void mismatchedApplicationNameDoesNotCauseMutation() {
        replies.add(new Reply(200, application("different-app", "different-client")));
        assertThrows(IAMException.class, () -> adapter.deleteApplication("dept", "service-app"));
        assertEquals(1, requests.size());
    }
    @Test
    void authenticatesUsingClientCredentialsAndProviderExpiry() throws Exception {
        replies.add(new Reply(200, "{\"access_token\":\"token\",\"expires_in\":120}"));
        Instant before = Instant.now();
        Instant expiry = adapter.authenticateClient("client:id", "secret+ value").orElseThrow();
        assertFalse(expiry.isBefore(before.plusSeconds(120)));
        assertFalse(expiry.isAfter(Instant.now().plusSeconds(120)));
        Request request = requests.remove();
        assertEquals("client%3Aid:secret%2B+value", decodedAuth(request));
        assertEquals("grant_type=client_credentials", request.body());
        assertEquals("/oauth2/token", request.uri());
    }

    @Test
    void distinguishesInvalidCredentialsFromProviderFailure() throws Exception {
        replies.add(new Reply(401, "{\"error\":\"invalid_client\"}"));
        assertTrue(adapter.authenticateClient("id", "secret").isEmpty());
        replies.add(new Reply(500, "{\"error\":\"server_error\",\"secret\":\"do-not-leak\"}"));
        IAMException failure = assertThrows(IAMException.class, () -> adapter.authenticateClient("id", "secret"));
        assertTrue(failure.getMessage().contains("500"));
        assertFalse(failure.getMessage().contains("do-not-leak"));
        replies.add(new Reply(400, "{\"error\":\"unauthorized_client\"}"));
        assertThrows(IAMException.class, () -> adapter.authenticateClient("id", "secret"));
    }

    @Test
    void rejectsMalformedSuccessResponses() {
        replies.add(new Reply(200, "{\"access_token\":\"token\",\"expires_in\":-1}"));
        assertThrows(IAMException.class, () -> adapter.authenticateClient("id", "secret"));
        replies.add(new Reply(201, "{\"client_id\":\"id\"}"));
        assertThrows(IAMException.class, () -> adapter.createClient(registration(null)));
        replies.add(new Reply(200, "not-json"));
        assertThrows(IAMException.class, () -> adapter.authenticateClient("id", "secret"));
    }

    @Test
    void unsupportedMutationsDoNotContactProvider() {
        assertThrows(UnsupportedOperationException.class, () -> adapter.createDepartment("dept", null));
        assertThrows(UnsupportedOperationException.class, adapter::resetProvider);
        assertThrows(UnsupportedOperationException.class, () -> adapter.createUser(null));
        assertTrue(requests.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void transportFailureIsNotReportedAsInvalidCredentials() throws Exception {
        doThrow(new java.io.IOException("Connection failed")).when(http)
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        IAMException error = assertThrows(IAMException.class, () -> adapter.authenticateClient("id", "secret"));
        assertInstanceOf(java.io.IOException.class, error.getCause());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interruptionRestoresThreadInterruptFlag() throws Exception {
        doThrow(new InterruptedException()).when(http)
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        try {
            assertThrows(IAMException.class, () -> adapter.authenticateClient("id", "secret"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void applicationNameIsEncodedAsAQueryValue() throws Exception {
        String name = "name &client_name=other";
        replies.add(new Reply(200, application(name, "client-id")));
        replies.add(new Reply(204, ""));
        adapter.deleteApplication(null, name);
        assertEquals("/identity/register/1.0.0/register?client_name=name+%26client_name%3Dother",
                requests.remove().uri());
        assertEquals("/identity/register/1.0.0/register/client-id", requests.remove().uri());
    }

    private static ClientRegistration registration(String id) {
        return new ClientRegistration(id, "service-app", Set.of("client_credentials"), List.of("https://example.test/callback"));
    }

    @Test
    void gatewayTokenCanBeRefreshedBetweenRequests() throws Exception {
        var token = new java.util.concurrent.atomic.AtomicReference<>("first-token");
        var gatewayAdapter = new AutentikaAdapter(new AutentikaConfiguration(
                REGISTRATION_ENDPOINT,
                URI.create("https://autentika.example.test/oauth2/token"), token::get, Duration.ofSeconds(3)), http);
        replies.add(new Reply(204, ""));
        replies.add(new Reply(204, ""));
        gatewayAdapter.deleteClient("first-id");
        token.set("refreshed-token");
        gatewayAdapter.deleteClient("second-id");
        assertEquals("Bearer first-token", requests.remove().authorization());
        assertEquals("Bearer refreshed-token", requests.remove().authorization());
    }

    @Test
    void missingGatewayTokenFailsBeforeSendingRequest() {
        var gatewayAdapter = new AutentikaAdapter(new AutentikaConfiguration(
                REGISTRATION_ENDPOINT,
                URI.create("https://autentika.example.test/oauth2/token"), () -> "", Duration.ofSeconds(3)), http);
        assertThrows(IAMException.class, () -> gatewayAdapter.deleteClient("id"));
        verifyNoInteractions(http);
    }

    @Test
    void gateway404DoesNotPretendThatClientWasDeleted() {
        replies.add(new Reply(404, "<html>Not Found</html>"));
        IAMException failure = assertThrows(IAMException.class, () -> adapter.deleteClient("id"));
        assertTrue(failure.getMessage().contains("deletion could not be confirmed"));
        replies.add(new Reply(404, "{}"));
        assertThrows(IAMException.class, () -> adapter.deleteClient("id"));
    }

    private static String application(String name, String clientId) {
        return JsonMapper.builder().build().writeValueAsString(java.util.Map.of(
                "client_id", clientId, "client_name", name, "client_secret", "test-secret",
                "grant_types", List.of("authorization_code"),
                "redirect_uris", List.of("https://example.test/callback")));
    }

    private static String decodedAuth(Request request) {
        return new String(Base64.getDecoder().decode(request.authorization().substring(6)), StandardCharsets.UTF_8);
    }

    private record Reply(int status, String body) { }
    private record Request(String method, String uri, String authorization, String body) { }
}
