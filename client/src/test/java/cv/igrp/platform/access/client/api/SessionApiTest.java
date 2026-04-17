package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.SessionInitRequestDTO;
import cv.igrp.platform.access.client.model.SessionRefreshRequestDTO;
import cv.igrp.platform.access.client.model.SessionResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionApiTest {

    @Mock
    private IApiClient apiClient;

    private SessionApi sessionApi;

    private final SessionResponseDTO sampleSession = new SessionResponseDTO();

    @BeforeEach
    void setUp() {
        sessionApi = new SessionApi(apiClient);

        sampleSession.setSessionId("sess-abc-123");
        sampleSession.setClientIp("192.168.1.10");
        sampleSession.setDeviceId("device-001");
    }

    @Test
    void getCurrentSession_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(SessionResponseDTO.class)))
                .thenReturn(sampleSession);

        SessionResponseDTO result = sessionApi.getCurrentSession();

        assertNotNull(result);
        assertEquals("sess-abc-123", result.getSessionId());
        verify(apiClient).invokeAPI("/api/session", "GET", null, null, null, SessionResponseDTO.class);
    }

    @Test
    void getCurrentSession_ThrowsApiException() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(SessionResponseDTO.class)))
                .thenThrow(new ApiException(401, "Unauthorized"));

        assertThrows(ApiException.class, () -> sessionApi.getCurrentSession());
    }

    @Test
    void initializeSession_Success() throws ApiException {
        SessionInitRequestDTO request = new SessionInitRequestDTO("192.168.1.10", "device-001", "Mozilla/5.0");

        when(apiClient.invokeAPI(eq("/api/session/init"), eq("POST"), isNull(), eq(request), isNull(), eq(SessionResponseDTO.class)))
                .thenReturn(sampleSession);

        SessionResponseDTO result = sessionApi.initializeSession(request);

        assertNotNull(result);
        assertEquals("sess-abc-123", result.getSessionId());
        verify(apiClient).invokeAPI("/api/session/init", "POST", null, request, null, SessionResponseDTO.class);
    }

    @Test
    void initializeSession_NullRequest_ThrowsApiException() {
        ApiException exception = assertThrows(ApiException.class, () -> sessionApi.initializeSession(null));
        assertEquals(400, exception.getCode());
    }

    @Test
    void initializeSession_ThrowsApiException() throws ApiException {
        SessionInitRequestDTO request = new SessionInitRequestDTO("192.168.1.10", "device-001", "Mozilla/5.0");

        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), eq(request), isNull(), eq(SessionResponseDTO.class)))
                .thenThrow(new ApiException(500, "Internal Server Error"));

        assertThrows(ApiException.class, () -> sessionApi.initializeSession(request));
    }

    @Test
    void refreshSession_Success() throws ApiException {
        SessionRefreshRequestDTO request = new SessionRefreshRequestDTO(3600);

        when(apiClient.invokeAPI(eq("/api/session/refresh"), eq("POST"), isNull(), eq(request), isNull(), eq(SessionResponseDTO.class)))
                .thenReturn(sampleSession);

        SessionResponseDTO result = sessionApi.refreshSession(request);

        assertNotNull(result);
        assertEquals("sess-abc-123", result.getSessionId());
        verify(apiClient).invokeAPI("/api/session/refresh", "POST", null, request, null, SessionResponseDTO.class);
    }

    @Test
    void refreshSession_ThrowsApiException() throws ApiException {
        SessionRefreshRequestDTO request = new SessionRefreshRequestDTO(3600);

        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), eq(request), isNull(), eq(SessionResponseDTO.class)))
                .thenThrow(new ApiException(404, "Session Not Found"));

        assertThrows(ApiException.class, () -> sessionApi.refreshSession(request));
    }

    @Test
    void closeSession_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), isNull(), isNull(), eq(String.class)))
                .thenReturn("Session closed");

        String result = sessionApi.closeSession();

        assertEquals("Session closed", result);
        verify(apiClient).invokeAPI("/api/session/close", "POST", null, null, null, String.class);
    }

    @Test
    void closeSession_ThrowsApiException() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), isNull(), isNull(), eq(String.class)))
                .thenThrow(new ApiException(500, "Internal Server Error"));

        assertThrows(ApiException.class, () -> sessionApi.closeSession());
    }

    @Test
    void rotateSession_Success() throws ApiException {
        SessionInitRequestDTO request = new SessionInitRequestDTO("192.168.1.10", "device-001", "Mozilla/5.0");

        when(apiClient.invokeAPI(eq("/api/session/rotate"), eq("POST"), isNull(), eq(request), isNull(), eq(SessionResponseDTO.class)))
                .thenReturn(sampleSession);

        SessionResponseDTO result = sessionApi.rotateSession(request);

        assertNotNull(result);
        assertEquals("sess-abc-123", result.getSessionId());
        verify(apiClient).invokeAPI("/api/session/rotate", "POST", null, request, null, SessionResponseDTO.class);
    }

    @Test
    void rotateSession_NullRequest_ThrowsApiException() {
        ApiException exception = assertThrows(ApiException.class, () -> sessionApi.rotateSession(null));
        assertEquals(400, exception.getCode());
    }

    @Test
    void rotateSession_ThrowsApiException() throws ApiException {
        SessionInitRequestDTO request = new SessionInitRequestDTO("192.168.1.10", "device-001", "Mozilla/5.0");

        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), eq(request), isNull(), eq(SessionResponseDTO.class)))
                .thenThrow(new ApiException(500, "Internal Server Error"));

        assertThrows(ApiException.class, () -> sessionApi.rotateSession(request));
    }
}
