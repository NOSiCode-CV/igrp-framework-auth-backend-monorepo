package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.JsonUtil;
import cv.igrp.platform.access.client.model.SessionResponseDTO;
import cv.igrp.platform.access.client.model.SessionKillRequestDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AdminSessionApi {
    private IApiClient apiClient;

    public AdminSessionApi() {
        this(new ApiClient());
    }

    public AdminSessionApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // List sessions with optional status filtering and pagination
    public String listSessions(String status, Integer page, Integer size) throws ApiException, IOException, InterruptedException {
        Map<String, String> queryParams = new HashMap<>();
        if (status != null) queryParams.put("status", status);
        if (page != null) queryParams.put("page", page.toString());
        if (size != null) queryParams.put("size", size.toString());

        return apiClient.invokeAPIRaw(
                "/api/admin/sessions",
                "GET",
                queryParams,
                null,
                null
        );
    }

    // Get user session by external ID
    public SessionResponseDTO getUserSession(String userExternalId) throws ApiException {
        if (userExternalId == null) {
            throw new ApiException(400, "Missing the required parameter 'userExternalId'");
        }

        String path = "/api/admin/sessions/users/{userExternalId}"
                .replaceAll("\\{" + "userExternalId" + "}", userExternalId);

        return apiClient.invokeAPI(
                path,
                "GET",
                null,
                null,
                null,
                SessionResponseDTO.class
        );
    }

    // Kill a specific session by ID
    public String killSession(String sessionId, SessionKillRequestDTO request) throws ApiException {
        if (sessionId == null) {
            throw new ApiException(400, "Missing the required parameter 'sessionId'");
        }
        if (request == null) {
            throw new ApiException(400, "Missing the required parameter 'request'");
        }

        String path = "/api/admin/sessions/{sessionId}/kill"
                .replaceAll("\\{" + "sessionId" + "}", sessionId);

        return apiClient.invokeAPI(
                path,
                "POST",
                null,
                request,
                null,
                String.class
        );
    }

    // List sessions by role with optional filters
    public String listSessionsByRole(String roleCode, String departmentCode, String status, Integer page, Integer size)
            throws ApiException, IOException, InterruptedException {
        if (roleCode == null) {
            throw new ApiException(400, "Missing the required parameter 'roleCode'");
        }

        String path = "/api/admin/sessions/roles/{roleCode}"
                .replaceAll("\\{" + "roleCode" + "}", roleCode);

        Map<String, String> queryParams = new HashMap<>();
        if (departmentCode != null) queryParams.put("departmentCode", departmentCode);
        if (status != null) queryParams.put("status", status);
        if (page != null) queryParams.put("page", page.toString());
        if (size != null) queryParams.put("size", size.toString());

        return apiClient.invokeAPIRaw(
                path,
                "GET",
                queryParams,
                null,
                null
        );
    }

    // Kill sessions by role
    public String killSessionsByRole(String roleCode, String departmentCode, SessionKillRequestDTO request) throws ApiException {
        if (roleCode == null) {
            throw new ApiException(400, "Missing the required parameter 'roleCode'");
        }
        if (request == null) {
            throw new ApiException(400, "Missing the required parameter 'request'");
        }

        String path = "/api/admin/sessions/roles/{roleCode}/kill"
                .replaceAll("\\{" + "roleCode" + "}", roleCode);

        Map<String, String> queryParams = new HashMap<>();
        if (departmentCode != null) queryParams.put("departmentCode", departmentCode);

        return apiClient.invokeAPI(
                path,
                "POST",
                queryParams.isEmpty() ? null : queryParams,
                request,
                null,
                String.class
        );
    }

    // Kill sessions by department
    public String killSessionsByDepartment(String departmentCode, SessionKillRequestDTO request) throws ApiException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing the required parameter 'departmentCode'");
        }
        if (request == null) {
            throw new ApiException(400, "Missing the required parameter 'request'");
        }

        String path = "/api/admin/sessions/departments/{departmentCode}/kill"
                .replaceAll("\\{" + "departmentCode" + "}", departmentCode);

        return apiClient.invokeAPI(
                path,
                "POST",
                null,
                request,
                null,
                String.class
        );
    }

    // Get session statistics
    public String getSessionStatistics() throws ApiException, IOException, InterruptedException {
        return apiClient.invokeAPIRaw(
                "/api/admin/sessions/statistics",
                "GET",
                null,
                null,
                null
        );
    }
}
