package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.model.RoleDTO;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * Get a role by its ID
     *
     * @param id             The role ID
     * @param departmentCode The department code (required query parameter)
     * @return The role DTO
     * @throws ApiException if the API call fails
     */
    public RoleDTO getRoleById(Integer id, String departmentCode) throws ApiException {
        if (id == null) {
            throw new ApiException(400, "Missing required parameter 'id'");
        }
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }

        String path = "/api/roles/{id}".replaceAll("\\{" + "id" + "}", String.valueOf(id));

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("departmentCode", departmentCode);

        return apiClient.invokeAPI(path, "GET", queryParams, null, null, RoleDTO.class);
    }

}