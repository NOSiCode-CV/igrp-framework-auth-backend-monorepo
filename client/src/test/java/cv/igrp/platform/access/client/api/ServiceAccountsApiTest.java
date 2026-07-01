package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.ServiceAccountDTO;
import cv.igrp.platform.access.client.model.ServiceAccountRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceAccountsApiTest {

    @Mock
    private IApiClient apiClient;

    private ServiceAccountsApi api;

    private final ServiceAccountDTO sample = new ServiceAccountDTO();

    @BeforeEach
    void setUp() {
        api = new ServiceAccountsApi(apiClient);

        sample.setId("sa-uuid-1");
        sample.setName("Nightly Invoice ETL");
        sample.setDescription("Batch job that syncs invoices at 02:00");
        sample.setActive(true);
        sample.setOauthClientId("oc-uuid-1");
        sample.setClientId("etl-runner-m2m");
        sample.setApplicationId(42);
        sample.setApplicationCode("INV");
        sample.setRoleIds(new LinkedHashSet<>(List.of(10, 11)));
        sample.setRoleCodes(new LinkedHashSet<>(List.of("INV.invoice.reader", "INV.invoice.exporter")));
        sample.setPermissionIds(new LinkedHashSet<>(List.of(201, 202)));
        sample.setPermissionNames(new LinkedHashSet<>(List.of("my.invoice.approve", "my.invoice.delete")));
    }

    // ─── list ───────────────────────────────────────────────────────────

    @Test
    void listServiceAccounts_Success() throws Exception {
        when(apiClient.invokeAPIRaw(eq("/api/service-accounts"), eq("GET"), isNull(), isNull(), isNull()))
                .thenReturn("[{\"id\":\"sa-uuid-1\",\"name\":\"Nightly Invoice ETL\"}]");

        List<ServiceAccountDTO> result = api.listServiceAccounts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sa-uuid-1", result.get(0).getId());
        verify(apiClient).invokeAPIRaw("/api/service-accounts", "GET", null, null, null);
    }

    @Test
    void listServiceAccounts_PropagatesIOException() throws Exception {
        when(apiClient.invokeAPIRaw(anyString(), eq("GET"), any(), any(), any()))
                .thenThrow(new IOException("network"));

        assertThrows(IOException.class, () -> api.listServiceAccounts());
    }

    // ─── get by id ──────────────────────────────────────────────────────

    @Test
    void getServiceAccount_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(ServiceAccountDTO.class)))
                .thenReturn(sample);

        ServiceAccountDTO result = api.getServiceAccount("sa-uuid-1");

        assertNotNull(result);
        assertEquals("Nightly Invoice ETL", result.getName());
        assertEquals("oc-uuid-1", result.getOauthClientId());
        verify(apiClient).invokeAPI("/api/service-accounts/sa-uuid-1", "GET", null, null, null, ServiceAccountDTO.class);
    }

    @Test
    void getServiceAccount_NullId_ThrowsApiException() {
        ApiException ex = assertThrows(ApiException.class, () -> api.getServiceAccount(null));
        assertEquals(400, ex.getCode());
    }

    @Test
    void getServiceAccount_NotFound_PropagatesApiException() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(ServiceAccountDTO.class)))
                .thenThrow(new ApiException(404, "Not Found"));

        ApiException ex = assertThrows(ApiException.class, () -> api.getServiceAccount("missing"));
        assertEquals(404, ex.getCode());
    }

    // ─── create ─────────────────────────────────────────────────────────

    @Test
    void createServiceAccount_Success() throws ApiException {
        ServiceAccountRequestDTO request = new ServiceAccountRequestDTO();
        request.setName("Nightly Invoice ETL");
        request.setDescription("Batch job that syncs invoices at 02:00");
        request.setActive(true);
        request.setOauthClientId("oc-uuid-1");
        request.setApplicationId(42);
        request.setRoleIds(new LinkedHashSet<>(List.of(10, 11)));
        request.setPermissionIds(new LinkedHashSet<>(List.of(201, 202)));

        when(apiClient.invokeAPI(eq("/api/service-accounts"), eq("POST"), isNull(), eq(request), isNull(), eq(ServiceAccountDTO.class)))
                .thenReturn(sample);

        ServiceAccountDTO result = api.createServiceAccount(request);

        assertNotNull(result);
        assertEquals("sa-uuid-1", result.getId());
        assertEquals("etl-runner-m2m", result.getClientId());
        verify(apiClient).invokeAPI("/api/service-accounts", "POST", null, request, null, ServiceAccountDTO.class);
    }

    @Test
    void createServiceAccount_NullRequest_ThrowsApiException() {
        ApiException ex = assertThrows(ApiException.class, () -> api.createServiceAccount(null));
        assertEquals(400, ex.getCode());
    }

    // ─── update ─────────────────────────────────────────────────────────

    @Test
    void updateServiceAccount_Success_ReplacementSemantics() throws ApiException {
        // Simulate a PUT that drops one role and clears direct permissions —
        // the caller MUST send the intended full state, not a delta.
        ServiceAccountRequestDTO request = new ServiceAccountRequestDTO();
        request.setName(sample.getName());
        request.setDescription(sample.getDescription());
        request.setActive(true);
        request.setOauthClientId(sample.getOauthClientId());
        request.setApplicationId(sample.getApplicationId());
        request.setRoleIds(new LinkedHashSet<>(List.of(11)));      // 10 removed
        request.setPermissionIds(new LinkedHashSet<>());            // both dropped

        ServiceAccountDTO updated = new ServiceAccountDTO();
        updated.setId(sample.getId());
        updated.setRoleIds(new LinkedHashSet<>(List.of(11)));
        updated.setPermissionIds(new LinkedHashSet<>());

        when(apiClient.invokeAPI(anyString(), eq("PUT"), isNull(), eq(request), isNull(), eq(ServiceAccountDTO.class)))
                .thenReturn(updated);

        ServiceAccountDTO result = api.updateServiceAccount("sa-uuid-1", request);

        assertNotNull(result);
        assertEquals(Set.of(11), result.getRoleIds());
        assertTrue(result.getPermissionIds().isEmpty());
        verify(apiClient).invokeAPI("/api/service-accounts/sa-uuid-1", "PUT", null, request, null, ServiceAccountDTO.class);
    }

    @Test
    void updateServiceAccount_NullId_ThrowsApiException() {
        ApiException ex = assertThrows(ApiException.class,
                () -> api.updateServiceAccount(null, new ServiceAccountRequestDTO()));
        assertEquals(400, ex.getCode());
    }

    @Test
    void updateServiceAccount_NullRequest_ThrowsApiException() {
        ApiException ex = assertThrows(ApiException.class,
                () -> api.updateServiceAccount("sa-uuid-1", null));
        assertEquals(400, ex.getCode());
    }

    // ─── delete ─────────────────────────────────────────────────────────

    @Test
    void deleteServiceAccount_Success_ReturnsVoid() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("DELETE"), isNull(), isNull(), isNull(), eq(Void.class)))
                .thenReturn(null);

        assertDoesNotThrow(() -> api.deleteServiceAccount("sa-uuid-1"));
        verify(apiClient).invokeAPI("/api/service-accounts/sa-uuid-1", "DELETE", null, null, null, Void.class);
    }

    @Test
    void deleteServiceAccount_NullId_ThrowsApiException() {
        ApiException ex = assertThrows(ApiException.class, () -> api.deleteServiceAccount(null));
        assertEquals(400, ex.getCode());
    }

    @Test
    void deleteServiceAccount_NotFound_PropagatesApiException() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("DELETE"), isNull(), isNull(), isNull(), eq(Void.class)))
                .thenThrow(new ApiException(404, "Not Found"));

        ApiException ex = assertThrows(ApiException.class, () -> api.deleteServiceAccount("missing"));
        assertEquals(404, ex.getCode());
    }

    // ─── constructor plumbing ──────────────────────────────────────────

    @Test
    void constructor_UsesProvidedClient() {
        assertSame(apiClient, api.getIApiClient());
    }

    @Test
    void setIApiClient_ReplacesClient() {
        IApiClient replacement = mock(IApiClient.class);
        api.setIApiClient(replacement);
        assertSame(replacement, api.getIApiClient());
    }
}
