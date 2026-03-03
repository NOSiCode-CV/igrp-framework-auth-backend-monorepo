package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.model.RoleDTO;

public class RolesApi {
    private IApiClient apiClient;

    public RolesApi() {
        this(new ApiClient());
    }

    public RolesApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public RoleDTO getRoleByCode(String code) throws ApiException {
        if (code == null) {
            throw new ApiException(400, "Missing required parameter 'code'");
        }
        String path = "/api/roles/by-code/{code}".replaceAll("\\{" + "code" + "}", code);
        return apiClient.invokeAPI(path, "GET", null, null, null, RoleDTO.class);
    }

}