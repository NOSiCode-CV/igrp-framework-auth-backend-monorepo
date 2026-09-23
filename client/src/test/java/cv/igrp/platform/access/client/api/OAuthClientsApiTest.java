package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.OAuthClientDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthClientsApiTest {

    private static final String CLIENT_ID = "3f1d2c4e-0000-4000-8000-000000000001";

    @Mock
    private IApiClient apiClient;

    private OAuthClientsApi oauthClientsApi;

    @BeforeEach
    void setUp() {
        oauthClientsApi = new OAuthClientsApi(apiClient);
    }

    @Test
    void rotateOAuthClientSecret_PostsToRotateEndpoint() throws ApiException {
        OAuthClientDTO rotated = new OAuthClientDTO();
        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), isNull(), isNull(), eq(OAuthClientDTO.class)))
                .thenReturn(rotated);

        OAuthClientDTO result = oauthClientsApi.rotateOAuthClientSecret(CLIENT_ID);

        assertSame(rotated, result);
        verify(apiClient).invokeAPI(
                "/api/clients/" + CLIENT_ID + "/rotate-secret",
                "POST", null, null, null, OAuthClientDTO.class);
    }

    @Test
    void rotateOAuthClientSecret_RejectsNullId() {
        // Guard before any network call — a null id would otherwise POST to
        // /api/clients/null/rotate-secret.
        assertThrows(ApiException.class, () -> oauthClientsApi.rotateOAuthClientSecret(null));
        verifyNoInteractions(apiClient);
    }
}
