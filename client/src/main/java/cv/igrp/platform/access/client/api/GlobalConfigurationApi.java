package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.model.GlobalConfigurationDTO;

import java.util.Map;

public class GlobalConfigurationApi {
    private IApiClient apiClient;

    public GlobalConfigurationApi() {
        this(new ApiClient());
    }

    public GlobalConfigurationApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // GET /api/global-configuration?type={type}
    public GlobalConfigurationDTO getGlobalConfiguration(String type) throws ApiException {
        if (type == null || type.isBlank()) {
            throw new ApiException(400, "Missing required parameter 'type'");
        }

        Map<String, String> queryParams = Map.of("type", type);
        return apiClient.invokeAPI(
            "/api/global-configuration",
            "GET",
            queryParams,
            null,
            null,
            GlobalConfigurationDTO.class
        );
    }

    // POST /api/global-configuration
    public GlobalConfigurationDTO setGlobalConfiguration(GlobalConfigurationDTO configDTO) throws ApiException {
        if (configDTO == null) {
            throw new ApiException(400, "Missing required parameter 'configDTO'");
        }

        return apiClient.invokeAPI(
            "/api/global-configuration",
            "POST",
            null,
            configDTO,
            null,
            GlobalConfigurationDTO.class
        );
    }
}