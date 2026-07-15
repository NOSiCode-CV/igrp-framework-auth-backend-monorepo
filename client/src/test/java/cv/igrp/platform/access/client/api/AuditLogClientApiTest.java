package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.AuditChainValidationDTO;
import cv.igrp.platform.access.client.model.AuditLogDTO;
import cv.igrp.platform.access.client.model.AuditPurgeResultDTO;
import cv.igrp.platform.access.client.model.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogClientApiTest {

    @Mock
    private IApiClient apiClient;

    private AuditLogClientApi api;

    @BeforeEach
    void setUp() {
        api = new AuditLogClientApi(apiClient);
    }

    // --- list ---

    @Test
    @SuppressWarnings("unchecked")
    void list_deserialisesPageEnvelope_andForwardsFilters() throws ApiException, IOException, InterruptedException {
        String json = """
                {
                  "content": [
                    {"id":"11111111-1111-1111-1111-111111111111","sequenceNumber":1,
                     "eventType":"LOGIN_SUCCESS","category":"AUTHENTICATION","username":"alice"}
                  ],
                  "totalElements": 1, "totalPages": 1, "size": 20, "number": 0,
                  "first": true, "last": true, "empty": false
                }
                """;
        when(apiClient.invokeAPIRaw(eq("/api/auth/audit"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(json);

        PageResponse<AuditLogDTO> page = api.list(
                null, "alice", "LOGIN_SUCCESS", null, null, null, null, 0, 20, "timestamp,desc");

        assertEquals(1, page.getTotalElements());
        assertEquals("alice", page.getContent().get(0).getUsername());
        assertEquals("11111111-1111-1111-1111-111111111111", page.getContent().get(0).getId());

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(apiClient).invokeAPIRaw(eq("/api/auth/audit"), eq("GET"), captor.capture(), isNull(), isNull());
        Map<String, String> q = captor.getValue();
        assertEquals("alice", q.get("username"));
        assertEquals("LOGIN_SUCCESS", q.get("eventType"));
        assertEquals("0", q.get("page"));
        assertEquals("20", q.get("size"));
        assertEquals("timestamp,desc", q.get("sort"));
        assertFalse(q.containsKey("userId"));
    }

    @Test
    void list_noFilters_passesNullQueryMap() throws ApiException, IOException, InterruptedException {
        when(apiClient.invokeAPIRaw(eq("/api/auth/audit"), eq("GET"), isNull(), isNull(), isNull()))
                .thenReturn("{\"content\":[],\"totalElements\":0,\"empty\":true}");

        PageResponse<AuditLogDTO> page = api.list(
                null, null, null, null, null, null, null, null, null, null);

        assertTrue(page.isEmpty());
        assertNotNull(page.getContent());
        verify(apiClient).invokeAPIRaw("/api/auth/audit", "GET", null, null, null);
    }

    @Test
    void list_propagatesApiException() throws ApiException, IOException, InterruptedException {
        when(apiClient.invokeAPIRaw(anyString(), eq("GET"), any(), isNull(), isNull()))
                .thenThrow(new ApiException(403, "Forbidden"));

        assertThrows(ApiException.class, () -> api.list(
                null, null, null, null, null, null, null, 0, 20, null));
    }

    // --- validate ---

    @Test
    void validate_returnsTypedResult() throws ApiException {
        AuditChainValidationDTO stub = new AuditChainValidationDTO();
        stub.setValid(false);
        stub.setRowsChecked(42);
        stub.setBrokenAt(7L);
        when(apiClient.invokeAPI(eq("/api/auth/audit/validate"), eq("POST"),
                isNull(), isNull(), isNull(), eq(AuditChainValidationDTO.class)))
                .thenReturn(stub);

        AuditChainValidationDTO result = api.validate();

        assertFalse(result.isValid());
        assertEquals(42, result.getRowsChecked());
        assertEquals(7L, result.getBrokenAt());
        verify(apiClient).invokeAPI("/api/auth/audit/validate", "POST",
                null, null, null, AuditChainValidationDTO.class);
    }

    // --- purge ---

    @Test
    void purge_returnsRemovedCount() throws ApiException {
        AuditPurgeResultDTO stub = new AuditPurgeResultDTO();
        stub.setPurged(128);
        when(apiClient.invokeAPI(eq("/api/auth/audit/purge"), eq("DELETE"),
                isNull(), isNull(), isNull(), eq(AuditPurgeResultDTO.class)))
                .thenReturn(stub);

        AuditPurgeResultDTO result = api.purge();

        assertEquals(128, result.getPurged());
        verify(apiClient).invokeAPI("/api/auth/audit/purge", "DELETE",
                null, null, null, AuditPurgeResultDTO.class);
    }
}
