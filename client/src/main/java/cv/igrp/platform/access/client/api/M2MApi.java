package cv.igrp.platform.access.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.JsonUtil;
import cv.igrp.platform.access.client.model.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class M2MApi {
    private IApiClient apiClient;

    public M2MApi() {
        this(new ApiClient());
    }

    public M2MApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public Object syncPermissions(List<PermissionDTO> permissions, String m2mToken, String serviceName) throws ApiException, IOException, InterruptedException {

        Map<String, String> headers = new HashMap<>();

        if (serviceName != null) {
            headers.put("X-Machine-Service-ID", serviceName);
        }

        if (m2mToken != null) {
            headers.put("X-Machine-Auth-Token", m2mToken);
        }

        return apiClient.invokeAPIRaw("/api/m2m/sync/permissions", "POST", null, Objects.nonNull(permissions) ? permissions : List.of(), headers);
    }

    public Object syncResources(ResourceDTO resource, String m2mToken, String serviceName) throws ApiException, IOException, InterruptedException {

        Map<String, String> headers = new HashMap<>();

        if (serviceName != null) {
            headers.put("X-Machine-Service-ID", serviceName);
        }

        if (m2mToken != null) {
            headers.put("X-Machine-Auth-Token", m2mToken);
        }

        return apiClient.invokeAPIRaw("/api/m2m/sync/resources", "POST", null, resource, headers);
    }

    public Object syncApplication(ApplicationDTO application, String m2mToken, String serviceName) throws ApiException, IOException, InterruptedException {

        Map<String, String> headers = new HashMap<>();

        if (serviceName != null) {
            headers.put("X-Machine-Service-ID", serviceName);
        }

        if (m2mToken != null) {
            headers.put("X-Machine-Auth-Token", m2mToken);
        }

        return apiClient.invokeAPIRaw("/api/m2m/sync/applications", "POST", null, application, headers);
    }

    public Object syncApplicationMenus(String applicationCode, List<MenuEntryDTO> menus, String m2mToken, String serviceName) throws ApiException, IOException, InterruptedException {

        Map<String, String> headers = new HashMap<>();

        if (serviceName != null) {
            headers.put("X-Machine-Service-ID", serviceName);
        }

        if (m2mToken != null) {
            headers.put("X-Machine-Auth-Token", m2mToken);
        }

        return apiClient.invokeAPIRaw("/api/m2m/sync/applications/%s/menus".formatted(applicationCode), "POST", null, menus, headers);

    }

    public List<IGRPUserDTO> getUsers(boolean activeOnly, String applicationCode, String departmentCode, String roleCode, String permissionName, boolean includeChildrenDepartments, boolean includeChildrenRoles, List<String> identifiers, String m2mToken, String serviceName) throws ApiException, IOException, InterruptedException {

        Map<String, String> headers = new HashMap<>();

        if (serviceName != null) {
            headers.put("X-Machine-Service-ID", serviceName);
        }

        if (m2mToken != null) {
            headers.put("X-Machine-Auth-Token", m2mToken);
        }

        Map<String, String> queryParams = new HashMap<>();
        if (activeOnly) queryParams.put("activeOnly", "true");
        if (applicationCode != null) queryParams.put("applicationCode", applicationCode);
        if (departmentCode != null) queryParams.put("departmentCode", departmentCode);
        if (roleCode != null) queryParams.put("roleCode", roleCode);
        if (permissionName != null) queryParams.put("permissionName", permissionName);
        if (includeChildrenDepartments) queryParams.put("includeChildrenDepartments", "true");
        if (includeChildrenRoles) queryParams.put("includeChildrenRoles", "true");
        String response = apiClient.invokeAPIRaw("/api/m2m/users", "POST", queryParams, Objects.nonNull(identifiers) ? identifiers : List.of(), headers);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<DepartmentDTO> getDepartments(boolean activeOnly, String parentCode, boolean includeChildrenDepartments, List<String> identifiers, String m2mToken, String serviceName) throws IOException, InterruptedException, ApiException {

        Map<String, String> headers = new HashMap<>();

        if (serviceName != null) {
            headers.put("X-Machine-Service-ID", serviceName);
        }

        if (m2mToken != null) {
            headers.put("X-Machine-Auth-Token", m2mToken);
        }

        Map<String, String> queryParams = new HashMap<>();

        if (activeOnly) queryParams.put("activeOnly", "true");
        if (parentCode != null) queryParams.put("parentCode", parentCode);
        if (includeChildrenDepartments) queryParams.put("includeChildrenDepartments", "true");

        String response = apiClient.invokeAPIRaw("/api/m2m/departments", "POST", queryParams, Objects.nonNull(identifiers) ? identifiers : List.of(), headers);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<RoleDTO> getRoles(boolean activeOnly, String parentCode, boolean includeChildrenRoles, List<String> identifiers, String m2mToken, String serviceName) throws IOException, InterruptedException, ApiException {

        Map<String, String> headers = new HashMap<>();

        if (serviceName != null) {
            headers.put("X-Machine-Service-ID", serviceName);
        }

        if (m2mToken != null) {
            headers.put("X-Machine-Auth-Token", m2mToken);
        }

        Map<String, String> queryParams = new HashMap<>();

        if (activeOnly) queryParams.put("activeOnly", "true");
        if (parentCode != null) queryParams.put("parentCode", parentCode);
        if (includeChildrenRoles) queryParams.put("includeChildrenRoles", "true");

        String response = apiClient.invokeAPIRaw("/api/m2m/roles", "POST", queryParams, Objects.nonNull(identifiers) ? identifiers : List.of(), headers);
        return JsonUtil.deserialize(response, new TypeReference<>() {});

    }

}