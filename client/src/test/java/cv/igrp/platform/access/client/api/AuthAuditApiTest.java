package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.SecurityAuditLogDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthAuditApiTest {

    @Mock
    private IApiClient apiClient;

    private AuthAuditApi authAuditApi;

    private final SecurityAuditLogDTO sampleLog = new SecurityAuditLogDTO();

    @BeforeEach
    void setUp() {
        authAuditApi = new AuthAuditApi(apiClient);

        sampleLog.setId(1L);
        sampleLog.setUserId("user-001");
        sampleLog.setUsername("testuser");
        sampleLog.setEventType("LOGIN");
        sampleLog.setCategory("AUTHENTICATION");
        sampleLog.setIpAddress("192.168.1.10");
    }

    // --- listAuditLogs ---

    @Test
    void listAuditLogs_Success() throws ApiException, IOException, InterruptedException {
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("page", "0");
        expectedParams.put("size", "20");
        expectedParams.put("sort", "timestamp,desc");

        when(apiClient.invokeAPIRaw(eq("/api/auth/audit"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn("[{\"id\":1},{\"id\":2}]");

        String result = authAuditApi.listAuditLogs(0, 20, "timestamp,desc");

        assertNotNull(result);
        verify(apiClient).invokeAPIRaw("/api/auth/audit", "GET", expectedParams, null, null);
    }

    @Test
    void listAuditLogs_NoParams() throws ApiException, IOException, InterruptedException {
        when(apiClient.invokeAPIRaw(eq("/api/auth/audit"), eq("GET"), isNull(), isNull(), isNull()))
                .thenReturn("[]");

        String result = authAuditApi.listAuditLogs(null, null, null);

        assertNotNull(result);
        verify(apiClient).invokeAPIRaw("/api/auth/audit", "GET", null, null, null);
    }

    @Test
    void listAuditLogs_ThrowsApiException() throws ApiException, IOException, InterruptedException {
        when(apiClient.invokeAPIRaw(anyString(), eq("GET"), any(), isNull(), isNull()))
                .thenThrow(new ApiException(500, "Internal Server Error"));

        assertThrows(ApiException.class, () -> authAuditApi.listAuditLogs(0, 20, null));
    }

    // --- getAuditLogById ---

    @Test
    void getAuditLogById_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(SecurityAuditLogDTO.class)))
                .thenReturn(sampleLog);

        SecurityAuditLogDTO result = authAuditApi.getAuditLogById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("LOGIN", result.getEventType());
        verify(apiClient).invokeAPI("/api/auth/audit/1", "GET", null, null, null, SecurityAuditLogDTO.class);
    }

    @Test
    void getAuditLogById_NullId_ThrowsApiException() {
        ApiException exception = assertThrows(ApiException.class, () -> authAuditApi.getAuditLogById(null));
        assertEquals(400, exception.getCode());
    }

    @Test
    void getAuditLogById_ThrowsApiException() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(SecurityAuditLogDTO.class)))
                .thenThrow(new ApiException(404, "Not Found"));

        assertThrows(ApiException.class, () -> authAuditApi.getAuditLogById(999L));
    }

    // --- getAuditLogsByUserId ---

    @Test
    void getAuditLogsByUserId_Success() throws ApiException, IOException, InterruptedException {
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("page", "0");
        expectedParams.put("size", "10");
        expectedParams.put("sort", "timestamp,desc");

        when(apiClient.invokeAPIRaw(anyString(), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn("[{\"id\":1,\"userId\":\"user-001\"}]");

        String result = authAuditApi.getAuditLogsByUserId("user-001", 0, 10, "timestamp,desc");

        assertNotNull(result);
        verify(apiClient).invokeAPIRaw(
                "/api/auth/audit/user/user-001", "GET", expectedParams, null, null
        );
    }

    @Test
    void getAuditLogsByUserId_NullUserId_ThrowsApiException() {
        ApiException exception = assertThrows(ApiException.class,
                () -> authAuditApi.getAuditLogsByUserId(null, 0, 10, null));
        assertEquals(400, exception.getCode());
    }

    @Test
    void getAuditLogsByUserId_NoOptionalParams() throws ApiException, IOException, InterruptedException {
        when(apiClient.invokeAPIRaw(anyString(), eq("GET"), isNull(), isNull(), isNull()))
                .thenReturn("[]");

        String result = authAuditApi.getAuditLogsByUserId("user-001", null, null, null);

        assertNotNull(result);
        verify(apiClient).invokeAPIRaw(
                "/api/auth/audit/user/user-001", "GET", null, null, null
        );
    }

    @Test
    void getAuditLogsByUserId_ThrowsApiException() throws ApiException, IOException, InterruptedException {
        when(apiClient.invokeAPIRaw(anyString(), eq("GET"), any(), isNull(), isNull()))
                .thenThrow(new ApiException(500, "Internal Server Error"));

        assertThrows(ApiException.class, () -> authAuditApi.getAuditLogsByUserId("user-001", 0, 10, null));
    }
}
