package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.JsonUtil;
import cv.igrp.platform.access.client.model.PermissionCheckRequestDTO;
import cv.igrp.platform.access.client.model.PermissionCheckResponseDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AuthorizeApi {
    private IApiClient apiClient;

    public AuthorizeApi() {
        this(new ApiClient());
    }

    public AuthorizeApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Check if a user is authorized for a specific action on a resource
     *
     * @param request Permission check request containing resource and action
     * @return Permission check response with authorization details
     * @throws ApiException if the API call fails
     */
    public PermissionCheckResponseDTO checkAuthorization(PermissionCheckRequestDTO request) throws ApiException {
        if (request == null) {
            throw new ApiException(400, "Missing the required parameter 'request'");
        }

        return apiClient.invokeAPI(
                "/api/authorize/check",
                "POST",
                null,
                request,
                null,
                PermissionCheckResponseDTO.class
        );
    }

    /**
     * Batch check multiple authorization requests
     *
     * @param requests List of permission check requests
     * @return List of authorization results
     * @throws ApiException if the API call fails
     */
    public List<PermissionCheckResponseDTO> batchCheckAuthorization(List<PermissionCheckRequestDTO> requests) throws ApiException, IOException, InterruptedException {
        if (requests == null || requests.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'requests'");
        }

        String response = apiClient.invokeAPIRaw(
                "/api/authorize/batch-check",
                "POST",
                null,
                requests,
                null
        );

        return JsonUtil.deserialize(response, new TypeReference<>() {});

    }
}