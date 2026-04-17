package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.model.SessionResponseDTO;
import cv.igrp.platform.access.client.model.SessionInitRequestDTO;
import cv.igrp.platform.access.client.model.SessionRefreshRequestDTO;

public class SessionApi {
    private IApiClient apiClient;

    public SessionApi() {
        this(new ApiClient());
    }

    public SessionApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // Get current session for the authenticated user
    public SessionResponseDTO getCurrentSession() throws ApiException {
        return apiClient.invokeAPI(
                "/api/session",
                "GET",
                null,
                null,
                null,
                SessionResponseDTO.class
        );
    }

    // Initialize a new session
    public SessionResponseDTO initializeSession(SessionInitRequestDTO request) throws ApiException {
        if (request == null) {
            throw new ApiException(400, "Missing the required parameter 'request'");
        }

        return apiClient.invokeAPI(
                "/api/session/init",
                "POST",
                null,
                request,
                null,
                SessionResponseDTO.class
        );
    }

    // Refresh the current session
    public SessionResponseDTO refreshSession(SessionRefreshRequestDTO request) throws ApiException {
        return apiClient.invokeAPI(
                "/api/session/refresh",
                "POST",
                null,
                request,
                null,
                SessionResponseDTO.class
        );
    }

    // Close the current session
    public String closeSession() throws ApiException {
        return apiClient.invokeAPI(
                "/api/session/close",
                "POST",
                null,
                null,
                null,
                String.class
        );
    }

    // Rotate the current session (close and create new)
    public SessionResponseDTO rotateSession(SessionInitRequestDTO request) throws ApiException {
        if (request == null) {
            throw new ApiException(400, "Missing the required parameter 'request'");
        }

        return apiClient.invokeAPI(
                "/api/session/rotate",
                "POST",
                null,
                request,
                null,
                SessionResponseDTO.class
        );
    }
}
