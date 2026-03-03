package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.PermissionCheckRequestDTO;
import cv.igrp.platform.access.client.model.PermissionCheckResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizeApiTest {

    @Mock
    private IApiClient apiClient;

    private AuthorizeApi authorizeApi;
    private PermissionCheckRequestDTO sampleRequest;
    private PermissionCheckResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        authorizeApi = new AuthorizeApi(apiClient);

        sampleRequest = new PermissionCheckRequestDTO();
        sampleRequest.setResource("projects");
        sampleRequest.setAction("create");

        sampleResponse = new PermissionCheckResponseDTO();
        sampleResponse.setAllowed(true);
        sampleResponse.setCacheHit(false);
        sampleResponse.setResolutionTimeMs(15);
        sampleResponse.setViaRoles(Arrays.asList("admin", "project_manager"));
    }

    @Test
    void checkAuthorization_Success() throws ApiException {
        when(apiClient.invokeAPI(
                eq("/api/authorize/check"),
                eq("POST"),
                isNull(),
                eq(sampleRequest),
                isNull(),
                eq(PermissionCheckResponseDTO.class)
        )).thenReturn(sampleResponse);

        PermissionCheckResponseDTO response = authorizeApi.checkAuthorization(sampleRequest);

        assertTrue(response.isAllowed());
        assertEquals(2, response.getViaRoles().size());
        verify(apiClient).invokeAPI(
                "/api/authorize/check",
                "POST",
                null,
                sampleRequest,
                null,
                PermissionCheckResponseDTO.class
        );
    }

    @Test
    void checkAuthorization_ThrowsOnNullRequest() {
        assertThrows(ApiException.class, () -> authorizeApi.checkAuthorization(null));
    }

    @Test
    void batchCheckAuthorization_Success() throws ApiException, IOException, InterruptedException {
        List<PermissionCheckRequestDTO> requests = Arrays.asList(
                new PermissionCheckRequestDTO("projects", "read"),
                new PermissionCheckRequestDTO("users", "delete")
        );

        Map<String, PermissionCheckResponseDTO> mockResponse = new HashMap<>();
        mockResponse.put("projects:read", sampleResponse);

        when(apiClient.invokeAPIRaw(
                eq("/api/authorize/batch-check"),
                eq("POST"),
                isNull(),
                eq(requests),
                isNull())
        ).thenReturn("[{\"allowed\":true,\"viaRoles\":[\"admin\"],\"cacheHit\":false,\"resolutionTimeMs\":10}]");

        List<PermissionCheckResponseDTO> response = authorizeApi.batchCheckAuthorization(requests);

        assertFalse(response.isEmpty());
        assertTrue(response.stream().anyMatch(PermissionCheckResponseDTO::isAllowed));
        verify(apiClient).invokeAPIRaw(
                "/api/authorize/batch-check",
                "POST",
                null,
                requests,
                null
        );
    }

    @Test
    void batchCheckAuthorization_ThrowsOnEmptyRequest() {
        assertThrows(ApiException.class, () -> authorizeApi.batchCheckAuthorization(List.of()));
    }
}