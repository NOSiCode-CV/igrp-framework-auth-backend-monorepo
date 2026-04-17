package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.SessionKillRequestDTO;
import cv.igrp.platform.access.client.model.SessionResponseDTO;
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
class AdminSessionApiTest {

    @Mock
    private IApiClient apiClient;

    private AdminSessionApi adminSessionApi;

    private final SessionResponseDTO sampleSession = new SessionResponseDTO();
    private final SessionKillRequestDTO sampleKillRequest = new SessionKillRequestDTO("Security breach", "admin");

    @BeforeEach
    void setUp() {
        adminSessionApi = new AdminSessionApi(apiClient);

        sampleSession.setSessionId("sess-abc-123");
        sampleSession.setClientIp("192.168.1.10");
    }

    // --- listSessions ---

    @Test
    void listSessions_Success() throws ApiException, IOException, InterruptedException {
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("status", "ACTIVE");
        expectedParams.put("page", "0");
        expectedParams.put("size", "10");

        when(apiClient.invokeAPIRaw(eq("/api/admin/sessions"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn("[{\"sessionId\":\"sess-1\"},{\"sessionId\":\"sess-2\"}]");

        String result = adminSessionApi.listSessions("ACTIVE", 0, 10);

        assertNotNull(result);
        verify(apiClient).invokeAPIRaw("/api/admin/sessions", "GET", expectedParams, null, null);
    }

    @Test
    void listSessions_NoFilters() throws ApiException, IOException, InterruptedException {
        Map<String, String> expectedParams = new HashMap<>();

        when(apiClient.invokeAPIRaw(eq("/api/admin/sessions"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn("[]");

        String result = adminSessionApi.listSessions(null, null, null);

        assertNotNull(result);
        verify(apiClient).invokeAPIRaw("/api/admin/sessions", "GET", expectedParams, null, null);
    }

    @Test
    void listSessions_ThrowsApiException() throws ApiException, IOException, InterruptedException {
        when(apiClient.invokeAPIRaw(anyString(), eq("GET"), anyMap(), isNull(), isNull()))
                .thenThrow(new ApiException(500, "Internal Server Error"));

        assertThrows(ApiException.class, () -> adminSessionApi.listSessions("ACTIVE", 0, 10));
    }

    // --- getUserSession ---

    @Test
    void getUserSession_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(SessionResponseDTO.class)))
                .thenReturn(sampleSession);

        SessionResponseDTO result = adminSessionApi.getUserSession("user-ext-001");

        assertNotNull(result);
        assertEquals("sess-abc-123", result.getSessionId());
        verify(apiClient).invokeAPI(
                "/api/admin/sessions/users/user-ext-001", "GET", null, null, null, SessionResponseDTO.class
        );
    }

    @Test
    void getUserSession_NullParam_ThrowsApiException() {
        ApiException exception = assertThrows(ApiException.class, () -> adminSessionApi.getUserSession(null));
        assertEquals(400, exception.getCode());
    }

    @Test
    void getUserSession_ThrowsApiException() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(SessionResponseDTO.class)))
                .thenThrow(new ApiException(404, "Not Found"));

        assertThrows(ApiException.class, () -> adminSessionApi.getUserSession("user-ext-001"));
    }

    // --- killSession ---

    @Test
    void killSession_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), eq(sampleKillRequest), isNull(), eq(String.class)))
                .thenReturn("Session killed");

        String result = adminSessionApi.killSession("sess-abc-123", sampleKillRequest);

        assertEquals("Session killed", result);
        verify(apiClient).invokeAPI(
                "/api/admin/sessions/sess-abc-123/kill", "POST", null, sampleKillRequest, null, String.class
        );
    }

    @Test
    void killSession_NullSessionId_ThrowsApiException() {
        ApiException exception = assertThrows(ApiException.class,
                () -> adminSessionApi.killSession(null, sampleKillRequest));
        assertEquals(400, exception.getCode());
    }

    @Test
    void killSession_NullRequest_ThrowsApiException() {
        ApiException exception = assertThrows(ApiException.class,
                () -> adminSessionApi.killSession("sess-abc-123", null));
        assertEquals(400, exception.getCode());
    }

    @Test
    void killSession_ThrowsApiException() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), eq(sampleKillRequest), isNull(), eq(String.class)))
                .thenThrow(new ApiException(404, "Session Not Found"));

        assertThrows(ApiException.class, () -> adminSessionApi.killSession("sess-abc-123", sampleKillRequest));
    }

    // --- listSessionsByRole ---

    @Test
    void listSessionsByRole_Success() throws ApiException, IOException, InterruptedException {
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("departmentCode", "DEPT01");
        expectedParams.put("status", "ACTIVE");
        expectedParams.put("page", "0");
        expectedParams.put("size", "20");

        when(apiClient.invokeAPIRaw(anyString(), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn("[{\"sessionId\":\"sess-1\"}]");

        String result = adminSessionApi.listSessionsByRole("ADMIN", "DEPT01", "ACTIVE", 0, 20);

        assertNotNull(result);
        verify(apiClient).invokeAPIRaw(
                "/api/admin/sessions/roles/ADMIN", "GET", expectedParams, null, null
        );
    }

    @Test
    void listSessionsByRole_NullRoleCode_ThrowsApiException() {
        ApiException exception = assertThrows(ApiException.class,
                () -> adminSessionApi.listSessionsByRole(null, null, null, null, null));
        assertEquals(400, exception.getCode());
    }

    @Test
    void listSessionsByRole_MinimalParams() throws ApiException, IOException, InterruptedException {
        Map<String, String> expectedParams = new HashMap<>();

        when(apiClient.invokeAPIRaw(anyString(), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn("[]");

        String result = adminSessionApi.listSessionsByRole("VIEWER", null, null, null, null);

        assertNotNull(result);
        verify(apiClient).invokeAPIRaw(
                "/api/admin/sessions/roles/VIEWER", "GET", expectedParams, null, null
        );
    }

    // --- killSessionsByRole ---

    @Test
    void killSessionsByRole_Success() throws ApiException {
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("departmentCode", "DEPT01");

        when(apiClient.invokeAPI(anyString(), eq("POST"), anyMap(), eq(sampleKillRequest), isNull(), eq(String.class)))
                .thenReturn("Sessions killed");

        String result = adminSessionApi.killSessionsByRole("ADMIN", "DEPT01", sampleKillRequest);

        assertEquals("Sessions killed", result);
        verify(apiClient).invokeAPI(
                "/api/admin/sessions/roles/ADMIN/kill", "POST", expectedParams, sampleKillRequest, null, String.class
        );
    }

    @Test
    void killSessionsByRole_NoDepartment() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), eq(sampleKillRequest), isNull(), eq(String.class)))
                .thenReturn("Sessions killed");

        String result = adminSessionApi.killSessionsByRole("ADMIN", null, sampleKillRequest);

        assertEquals("Sessions killed", result);
        verify(apiClient).invokeAPI(
                "/api/admin/sessions/roles/ADMIN/kill", "POST", null, sampleKillRequest, null, String.class
        );
    }

    @Test
    void killSessionsByRole_NullRoleCode_ThrowsApiException() {
        ApiException exception = assertThrows(ApiException.class,
                () -> adminSessionApi.killSessionsByRole(null, null, sampleKillRequest));
        assertEquals(400, exception.getCode());
    }

    @Test
    void killSessionsByRole_NullRequest_ThrowsApiException() {
        ApiException exception = assertThrows(ApiException.class,
                () -> adminSessionApi.killSessionsByRole("ADMIN", null, null));
        assertEquals(400, exception.getCode());
    }

    // --- killSessionsByDepartment ---

    @Test
    void killSessionsByDepartment_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), eq(sampleKillRequest), isNull(), eq(String.class)))
                .thenReturn("Sessions killed");

        String result = adminSessionApi.killSessionsByDepartment("DEPT01", sampleKillRequest);

        assertEquals("Sessions killed", result);
        verify(apiClient).invokeAPI(
                "/api/admin/sessions/departments/DEPT01/kill", "POST", null, sampleKillRequest, null, String.class
        );
    }

    @Test
    void killSessionsByDepartment_NullDepartmentCode_ThrowsApiException() {
        ApiException exception = assertThrows(ApiException.class,
                () -> adminSessionApi.killSessionsByDepartment(null, sampleKillRequest));
        assertEquals(400, exception.getCode());
    }

    @Test
    void killSessionsByDepartment_NullRequest_ThrowsApiException() {
        ApiException exception = assertThrows(ApiException.class,
                () -> adminSessionApi.killSessionsByDepartment("DEPT01", null));
        assertEquals(400, exception.getCode());
    }

    @Test
    void killSessionsByDepartment_ThrowsApiException() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), eq(sampleKillRequest), isNull(), eq(String.class)))
                .thenThrow(new ApiException(500, "Internal Server Error"));

        assertThrows(ApiException.class, () -> adminSessionApi.killSessionsByDepartment("DEPT01", sampleKillRequest));
    }

    // --- getSessionStatistics ---

    @Test
    void getSessionStatistics_Success() throws ApiException, IOException, InterruptedException {
        when(apiClient.invokeAPIRaw(eq("/api/admin/sessions/statistics"), eq("GET"), isNull(), isNull(), isNull()))
                .thenReturn("{\"activeSessions\":42,\"totalSessions\":100}");

        String result = adminSessionApi.getSessionStatistics();

        assertNotNull(result);
        verify(apiClient).invokeAPIRaw("/api/admin/sessions/statistics", "GET", null, null, null);
    }

    @Test
    void getSessionStatistics_ThrowsApiException() throws ApiException, IOException, InterruptedException {
        when(apiClient.invokeAPIRaw(anyString(), eq("GET"), isNull(), isNull(), isNull()))
                .thenThrow(new ApiException(500, "Internal Server Error"));

        assertThrows(ApiException.class, () -> adminSessionApi.getSessionStatistics());
    }
}
