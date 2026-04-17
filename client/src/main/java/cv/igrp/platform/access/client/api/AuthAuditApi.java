package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.model.SecurityAuditLogDTO;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthAuditApi {
    private IApiClient apiClient;

    public AuthAuditApi() {
        this(new ApiClient());
    }

    public AuthAuditApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // List all audit logs with pagination
    public String listAuditLogs(Integer page, Integer size, String sort) throws ApiException, IOException, InterruptedException {
        Map<String, String> queryParams = new HashMap<>();
        if (page != null) queryParams.put("page", page.toString());
        if (size != null) queryParams.put("size", size.toString());
        if (sort != null) queryParams.put("sort", sort);

        return apiClient.invokeAPIRaw(
                "/api/auth/audit",
                "GET",
                queryParams.isEmpty() ? null : queryParams,
                null,
                null
        );
    }

    // Get a specific audit log by ID
    public SecurityAuditLogDTO getAuditLogById(Long id) throws ApiException {
        if (id == null) {
            throw new ApiException(400, "Missing the required parameter 'id'");
        }

        String path = "/api/auth/audit/{id}"
                .replaceAll("\\{" + "id" + "}", id.toString());

        return apiClient.invokeAPI(
                path,
                "GET",
                null,
                null,
                null,
                SecurityAuditLogDTO.class
        );
    }

    // Get audit logs for a specific user
    public String getAuditLogsByUserId(String userId, Integer page, Integer size, String sort)
            throws ApiException, IOException, InterruptedException {
        if (userId == null) {
            throw new ApiException(400, "Missing the required parameter 'userId'");
        }

        String path = "/api/auth/audit/user/{userId}"
                .replaceAll("\\{" + "userId" + "}", userId);

        Map<String, String> queryParams = new HashMap<>();
        if (page != null) queryParams.put("page", page.toString());
        if (size != null) queryParams.put("size", size.toString());
        if (sort != null) queryParams.put("sort", sort);

        return apiClient.invokeAPIRaw(
                path,
                "GET",
                queryParams.isEmpty() ? null : queryParams,
                null,
                null
        );
    }
}
