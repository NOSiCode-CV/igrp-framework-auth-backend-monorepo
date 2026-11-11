package cv.igrp.framework.auth.core.autoconfig;

import cv.igrp.platform.access.client.ApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** v3-18 (#26): the legacy sync runner only ever authenticates with client_credentials. */
class AuthorizationSyncRunnerTest {

    private final ApiClient client = mock(ApiClient.class);
    private final AuthorizationSyncRunner runner = new AuthorizationSyncRunner(client);

    private void configure(String clientId, String clientSecret, String scope) {
        ReflectionTestUtils.setField(runner, "clientId", clientId);
        ReflectionTestUtils.setField(runner, "clientSecret", clientSecret);
        ReflectionTestUtils.setField(runner, "scope", scope);
        ReflectionTestUtils.setField(runner, "applicationName", "faturacao-api");
    }

    @Test
    @DisplayName("Without client credentials nothing is sent to AM: there is no static-token fallback")
    void skippedWithoutCredentials() {
        configure("", "", "");
        runner.syncAuthorization();
        verifyNoInteractions(client);

        configure("faturacao-api", " ", "");
        runner.syncAuthorization();
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("With client credentials, the resource and permissions are synced under the token")
    void syncedWithCredentials() throws Exception {
        configure("faturacao-api", "secret", "igrp.m2m");
        runner.syncAuthorization();

        verify(client).setClientCredentials("faturacao-api", "secret", "igrp.m2m");
        verify(client).invokeAPIRaw(eq("/api/m2m/sync/resources"), eq("POST"), any(), any(), any());
        verify(client).invokeAPIRaw(eq("/api/m2m/sync/permissions"), eq("POST"), any(), any(), any());
    }

    @Test
    @DisplayName("Without a scope the credentials are set without one")
    void noScope() {
        configure("faturacao-api", "secret", "");
        runner.syncAuthorization();

        verify(client).setClientCredentials("faturacao-api", "secret");
        verify(client, never()).setClientCredentials(anyString(), anyString(), anyString());
    }
}
