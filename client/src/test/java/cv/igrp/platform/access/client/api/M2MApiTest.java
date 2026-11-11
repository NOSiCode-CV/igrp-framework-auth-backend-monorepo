package cv.igrp.platform.access.client.api;

import tools.jackson.core.type.TypeReference;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.JsonUtil;
import cv.igrp.platform.access.client.auth.ClientCredentialsTokenProvider;
import cv.igrp.platform.access.client.model.ApplicationDTO;
import cv.igrp.platform.access.client.model.MenuEntryDTO;
import cv.igrp.platform.access.client.model.PermissionDTO;
import cv.igrp.platform.access.client.model.ResourceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class M2MApiTest {

    @Mock
    private IApiClient apiClient;

    private M2MApi m2mApi;
    private final PermissionDTO samplePermission = new PermissionDTO();
    private final ResourceDTO sampleResource = new ResourceDTO();
    private final ApplicationDTO sampleApplication = new ApplicationDTO();

    @BeforeEach
    void setUp() {
        m2mApi = new M2MApi(apiClient);

        // Setup sample permission
        samplePermission.setId(101);
        samplePermission.setName("test-permission");

        // Setup sample resource
        sampleResource.setId(201);
        sampleResource.setName("test-resource");

        // Setup sample application
        sampleApplication.setId(301);
        sampleApplication.setName("test-application");
    }

    @Test
    void syncPermissions_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "{\"success\":true,\"message\":\"Permissions synced successfully\"}";
        
        when(apiClient.invokeAPIRaw(
                eq("/api/m2m/sync/permissions"),
                eq("POST"),
                isNull(),
                anyList(),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        )).thenReturn(jsonResponse);

        Object result = m2mApi.syncPermissions(List.of(samplePermission), "svc");
        
        assertNotNull(result);
        verify(apiClient).invokeAPIRaw(
                eq("/api/m2m/sync/permissions"),
                eq("POST"),
                isNull(),
                eq(List.of(samplePermission)),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        );
    }

    @Test
    void syncPermissions_NullList_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "{\"success\":true,\"message\":\"Empty permissions list handled\"}";
        
        when(apiClient.invokeAPIRaw(
                eq("/api/m2m/sync/permissions"),
                eq("POST"),
                isNull(),
                eq(List.of()),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        )).thenReturn(jsonResponse);

        Object result = m2mApi.syncPermissions(null, "svc");
        
        assertNotNull(result);
        verify(apiClient).invokeAPIRaw(
                eq("/api/m2m/sync/permissions"),
                eq("POST"),
                isNull(),
                eq(List.of()),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        );
    }

    @Test
    void syncResources_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "{\"success\":true,\"message\":\"Resource synced successfully\"}";
        
        when(apiClient.invokeAPIRaw(
                eq("/api/m2m/sync/resources"),
                eq("POST"),
                isNull(),
                eq(sampleResource),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        )).thenReturn(jsonResponse);

        Object result = m2mApi.syncResources(sampleResource, "svc");
        
        assertNotNull(result);
        verify(apiClient).invokeAPIRaw(
                eq("/api/m2m/sync/resources"),
                eq("POST"),
                isNull(),
                eq(sampleResource),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        );
    }

    @Test
    void syncApplication_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "{\"success\":true,\"message\":\"Application synced successfully\"}";
        
        when(apiClient.invokeAPIRaw(
                eq("/api/m2m/sync/applications"),
                eq("POST"),
                isNull(),
                eq(sampleApplication),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        )).thenReturn(jsonResponse);

        Object result = m2mApi.syncApplication(sampleApplication, "svc");
        
        assertNotNull(result);
        verify(apiClient).invokeAPIRaw(
                eq("/api/m2m/sync/applications"),
                eq("POST"),
                isNull(),
                eq(sampleApplication),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        );
    }

    @Test
    void syncApplicationMenus_DefaultNoSyncRolesParam_Success() throws ApiException, IOException, InterruptedException {
        List<MenuEntryDTO> menus = List.of(new MenuEntryDTO());

        when(apiClient.invokeAPIRaw(
                eq("/api/m2m/sync/applications/APP1/menus"),
                eq("POST"),
                isNull(),
                eq(menus),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        )).thenReturn("");

        Object result = m2mApi.syncApplicationMenus("APP1", menus, "svc");

        assertNotNull(result);
        verify(apiClient).invokeAPIRaw(
                eq("/api/m2m/sync/applications/APP1/menus"),
                eq("POST"),
                isNull(),
                eq(menus),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        );
    }

    @Test
    void syncApplicationMenus_WithSyncRolesFalse_PassesQueryParam() throws ApiException, IOException, InterruptedException {
        List<MenuEntryDTO> menus = List.of(new MenuEntryDTO());

        when(apiClient.invokeAPIRaw(
                eq("/api/m2m/sync/applications/APP1/menus"),
                eq("POST"),
                eq(Map.of("syncRoles", "false")),
                eq(menus),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        )).thenReturn("");

        Object result = m2mApi.syncApplicationMenus("APP1", menus, Boolean.FALSE, "svc");

        assertNotNull(result);
        verify(apiClient).invokeAPIRaw(
                eq("/api/m2m/sync/applications/APP1/menus"),
                eq("POST"),
                eq(Map.of("syncRoles", "false")),
                eq(menus),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        );
    }

    @Test
    void syncApplicationMenus_WithSyncRolesTrue_PassesQueryParam() throws ApiException, IOException, InterruptedException {
        List<MenuEntryDTO> menus = List.of(new MenuEntryDTO());

        when(apiClient.invokeAPIRaw(
                eq("/api/m2m/sync/applications/APP1/menus"),
                eq("POST"),
                eq(Map.of("syncRoles", "true")),
                eq(menus),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        )).thenReturn("");

        m2mApi.syncApplicationMenus("APP1", menus, Boolean.TRUE, "svc");

        verify(apiClient).invokeAPIRaw(
                eq("/api/m2m/sync/applications/APP1/menus"),
                eq("POST"),
                eq(Map.of("syncRoles", "true")),
                eq(menus),
                eq(Map.of("X-Machine-Service-ID", "svc"))
        );
    }

    @Test
    void m2mCallsCarryTheClientCredentialsTokenAndNoStaticToken() throws Exception {
        ClientCredentialsTokenProvider tokens = mock(ClientCredentialsTokenProvider.class);
        when(tokens.getAccessToken()).thenReturn("access-token");
        when(apiClient.getClientCredentialsTokenProvider()).thenReturn(tokens);

        m2mApi.syncResources(sampleResource, "svc");

        verify(apiClient).invokeAPIRaw(eq("/api/m2m/sync/resources"), eq("POST"), isNull(), eq(sampleResource),
                eq(Map.of("X-Machine-Service-ID", "svc", "Authorization", "Bearer access-token")));
    }
}
