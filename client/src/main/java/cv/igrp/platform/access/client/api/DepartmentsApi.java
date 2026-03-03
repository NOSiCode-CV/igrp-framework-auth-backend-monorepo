package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.JsonUtil;
import cv.igrp.platform.access.client.constants.Status;
import cv.igrp.platform.access.client.model.*;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepartmentsApi {
    private IApiClient apiClient;

    public DepartmentsApi() {
        this(new ApiClient());
    }

    public DepartmentsApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // GET /api/departments/{id}
    public DepartmentDTO getDepartmentById(Integer id) throws ApiException {
        if (id == null) {
            throw new ApiException(400, "Missing required parameter 'id'");
        }
        String path = "/api/departments/{id}".replace("{id}", id.toString());
        return apiClient.invokeAPI(path, "GET", null, null, null, DepartmentDTO.class);
    }

    // GET /api/departments/by-code/{code}
    public DepartmentDTO getDepartmentByCode(String code) throws ApiException {
        if (code == null) {
            throw new ApiException(400, "Missing required parameter 'code'");
        }
        String path = "/api/departments/by-code/{code}".replace("{code}", code);
        return apiClient.invokeAPI(path, "GET", null, null, null, DepartmentDTO.class);
    }

    // PUT /api/departments/{code}
    public DepartmentDTO updateDepartment(String code, DepartmentDTO departmentDTO) throws ApiException {
        if (code == null || departmentDTO == null) {
            throw new ApiException(400, "Missing required parameters");
        }
        String path = "/api/departments/{code}".replace("{code}", code);
        return apiClient.invokeAPI(path, "PUT", null, departmentDTO, null, DepartmentDTO.class);
    }

    // DELETE /api/departments/{code}
    public String deleteDepartment(String code) throws ApiException {
        if (code == null) {
            throw new ApiException(400, "Missing required parameter 'code'");
        }
        String path = "/api/departments/{code}".replace("{code}", code);
        return apiClient.invokeAPI(path, "DELETE", null, null, null, String.class);
    }

    // POST /api/departments
    public DepartmentDTO createDepartment(DepartmentDTO departmentDTO) throws ApiException {
        if (departmentDTO == null) {
            throw new ApiException(400, "Missing required parameter 'departmentDTO'");
        }
        return apiClient.invokeAPI("/api/departments", "POST", null, departmentDTO, null, DepartmentDTO.class);
    }

    // GET /api/departments
    public List<DepartmentDTO> getDepartments(String name, Status status, String code, String parentCode) throws ApiException, IOException, InterruptedException {

        Map<String, String> queryParams = new HashMap<>();

        if (name != null) queryParams.put("name", name);
        if (status != null) queryParams.put("status", status.getCode());
        if (code != null) queryParams.put("code", code);
        if (parentCode != null) queryParams.put("parent_code", parentCode);

        String response = apiClient.invokeAPIRaw("/api/departments", "GET", queryParams, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    // GET /api/departments/{code}/resources/available
    public List<ResourceDTO> getResourcesAvailableForDepartment(String departmentCode) throws ApiException, IOException, InterruptedException {

        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }

        String path = "/api/departments/{code}/resources/available".replace("{code}", departmentCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    // GET /api/departments/{code}/menus/available
    public List<MenuEntryDTO> getMenusAvailableForDepartment(String departmentCode) throws ApiException, IOException, InterruptedException {

        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }

        String path = "/api/departments/{code}/menus/available".replace("{code}", departmentCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});

    }

    // GET /api/departments/{code}/applications/available
    public List<ApplicationDTO> getApplicationsAvailableForDepartment(String departmentCode) throws ApiException, IOException, InterruptedException {

        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }

        String path = "/api/departments/{code}/applications/available".replace("{code}", departmentCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});

    }

    // POST /api/departments/{code}/applications
    public String addApplicationsToDepartment(String departmentCode, List<String> applicationCodes) throws ApiException {

        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }

        if (applicationCodes == null || applicationCodes.isEmpty()) {
            throw new ApiException(400, "Missing required parameter 'applicationCodes'");
        }

        String path = "/api/departments/{code}/applications".replace("{code}", departmentCode);

        return apiClient.invokeAPI(path, "POST", null, applicationCodes, null, String.class);

    }

    // DELETE /api/departments/{code}/applications
    public String removeApplicationsToDepartment(String departmentCode, List<String> applicationCodes) throws ApiException {

        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }

        if (applicationCodes == null || applicationCodes.isEmpty()) {
            throw new ApiException(400, "Missing required parameter 'applicationCodes'");
        }

        String path = "/api/departments/{code}/applications".replace("{code}", departmentCode);

        return apiClient.invokeAPI(path, "DELETE", null, applicationCodes, null, String.class);

    }

    // POST /api/departments/{code}/menus
    public String addMenusToDepartment(String departmentCode, List<String> menuCodes) throws ApiException {

        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }

        if (menuCodes == null || menuCodes.isEmpty()) {
            throw new ApiException(400, "Missing required parameter 'menuCodes'");
        }

        String path = "/api/departments/{code}/menus".replace("{code}", departmentCode);

        return apiClient.invokeAPI(path, "POST", null, menuCodes, null, String.class);

    }

    // DELETE /api/departments/{code}/menus
    public String removeMenusToDepartment(String departmentCode, List<String> menuCodes) throws ApiException {

        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }

        if (menuCodes == null || menuCodes.isEmpty()) {
            throw new ApiException(400, "Missing required parameter 'menuCodes'");
        }

        String path = "/api/departments/{code}/menus".replace("{code}", departmentCode);

        return apiClient.invokeAPI(path, "DELETE", null, menuCodes, null, String.class);

    }

    // GET /api/departments/{code}/applications
    public List<ApplicationDTO> getDepartmentApplications(String departmentCode) throws ApiException, IOException, InterruptedException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        String path = "/api/departments/{code}/applications".replace("{code}", departmentCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    // Alias with proper naming for removing applications
    public String removeApplicationsFromDepartment(String departmentCode, List<String> applicationCodes) throws ApiException {
        return removeApplicationsToDepartment(departmentCode, applicationCodes);
    }

    // GET /api/departments/{departmentCode}/applications/{appCode}/menus/available
    public List<MenuEntryDTO> getAvailableMenus(String appCode, String departmentCode) throws ApiException, IOException, InterruptedException {
        if (appCode == null) {
            throw new ApiException(400, "Missing required parameter 'appCode'");
        }
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        String path = "/api/departments/{departmentCode}/applications/{appCode}/menus/available"
                .replace("{departmentCode}", departmentCode)
                .replace("{appCode}", appCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    // GET /api/departments/{departmentCode}/applications/{appCode}/menus
    public List<MenuEntryDTO> getDepartmentMenus(String appCode, String departmentCode) throws ApiException, IOException, InterruptedException {
        if (appCode == null) {
            throw new ApiException(400, "Missing required parameter 'appCode'");
        }
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        String path = "/api/departments/{departmentCode}/applications/{appCode}/menus"
                .replace("{departmentCode}", departmentCode)
                .replace("{appCode}", appCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    // POST /api/departments/{departmentCode}/applications/{appCode}/menus
    public String addMenusToDepartment(String appCode, String departmentCode, List<String> menuCodes) throws ApiException {
        if (appCode == null) {
            throw new ApiException(400, "Missing required parameter 'appCode'");
        }
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (menuCodes == null || menuCodes.isEmpty()) {
            throw new ApiException(400, "Missing required parameter 'menuCodes'");
        }
        String path = "/api/departments/{departmentCode}/applications/{appCode}/menus"
                .replace("{departmentCode}", departmentCode)
                .replace("{appCode}", appCode);
        return apiClient.invokeAPI(path, "POST", null, menuCodes, null, String.class);
    }

    // DELETE /api/departments/{departmentCode}/applications/{appCode}/menus
    public String removeMenusFromDepartment(String appCode, String departmentCode, List<String> menuCodes) throws ApiException {
        if (appCode == null) {
            throw new ApiException(400, "Missing required parameter 'appCode'");
        }
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (menuCodes == null || menuCodes.isEmpty()) {
            throw new ApiException(400, "Missing required parameter 'menuCodes'");
        }
        String path = "/api/departments/{departmentCode}/applications/{appCode}/menus"
                .replace("{departmentCode}", departmentCode)
                .replace("{appCode}", appCode);
        return apiClient.invokeAPI(path, "DELETE", null, menuCodes, null, String.class);
    }

    // GET /api/departments/{code}/resources
    public List<ResourceDTO> getDepartmentResources(String departmentCode) throws ApiException, IOException, InterruptedException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        String path = "/api/departments/{code}/resources".replace("{code}", departmentCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    // POST /api/departments/{code}/resources
    public String addResourcesToDepartment(String departmentCode, List<String> resourceCodes) throws ApiException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (resourceCodes == null || resourceCodes.isEmpty()) {
            throw new ApiException(400, "Missing required parameter 'resourceCodes'");
        }
        String path = "/api/departments/{code}/resources".replace("{code}", departmentCode);
        return apiClient.invokeAPI(path, "POST", null, resourceCodes, null, String.class);
    }

    // DELETE /api/departments/{code}/resources
    public String removeResourcesFromDepartment(String departmentCode, List<String> resourceCodes) throws ApiException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (resourceCodes == null || resourceCodes.isEmpty()) {
            throw new ApiException(400, "Missing required parameter 'resourceCodes'");
        }
        String path = "/api/departments/{code}/resources".replace("{code}", departmentCode);
        return apiClient.invokeAPI(path, "DELETE", null, resourceCodes, null, String.class);
    }

    // GET /api/departments/{code}/permissions
    public List<PermissionDTO> getDepartmentPermissions(String departmentCode) throws ApiException, IOException, InterruptedException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        String path = "/api/departments/{code}/permissions".replace("{code}", departmentCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    // GET /api/departments/{code}/permissions/available
    public List<PermissionDTO> getAvailablePermissions(String departmentCode) throws ApiException, IOException, InterruptedException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        String path = "/api/departments/{code}/permissions/available".replace("{code}", departmentCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    // POST /api/departments/{code}/permissions
    public String addPermissionsToDepartment(String departmentCode, List<String> permissionCodes) throws ApiException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            throw new ApiException(400, "Missing required parameter 'permissionCodes'");
        }
        String path = "/api/departments/{code}/permissions".replace("{code}", departmentCode);
        return apiClient.invokeAPI(path, "POST", null, permissionCodes, null, String.class);
    }

    // DELETE /api/departments/{code}/permissions
    public String removePermissionsFromDepartment(String departmentCode, List<String> permissionCodes) throws ApiException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            throw new ApiException(400, "Missing required parameter 'permissionCodes'");
        }
        String path = "/api/departments/{code}/permissions".replace("{code}", departmentCode);
        return apiClient.invokeAPI(path, "DELETE", null, permissionCodes, null, String.class);
    }

    // GET /api/departments/{departmentCode}/roles
    public List<RoleDTO> getRoles(String departmentCode, String roleCode) throws ApiException, IOException, InterruptedException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        Map<String, String> queryParams = null;
        if (roleCode != null) {
            queryParams = new HashMap<>();
            queryParams.put("roleCode", roleCode);
        }
        String path = "/api/departments/{departmentCode}/roles".replace("{departmentCode}", departmentCode);
        String response = apiClient.invokeAPIRaw(path, "GET", queryParams, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    // POST /api/departments/{departmentCode}/roles
    public RoleDTO createRole(String departmentCode, RoleDTO role) throws ApiException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (role == null) {
            throw new ApiException(400, "Missing required parameter 'role'");
        }
        String path = "/api/departments/{departmentCode}/roles".replace("{departmentCode}", departmentCode);
        return apiClient.invokeAPI(path, "POST", null, role, null, RoleDTO.class);
    }

    // PUT /api/departments/{departmentCode}/roles/{roleCode}
    public RoleDTO updateRole(String departmentCode, String roleCode, RoleDTO role) throws ApiException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (roleCode == null) {
            throw new ApiException(400, "Missing required parameter 'roleCode'");
        }
        if (role == null) {
            throw new ApiException(400, "Missing required parameter 'role'");
        }
        String path = "/api/departments/{departmentCode}/roles/{roleCode}".replace("{departmentCode}", departmentCode).replace("{roleCode}", roleCode);
        return apiClient.invokeAPI(path, "PUT", null, role, null, RoleDTO.class);
    }

    // DELETE /api/departments/{departmentCode}/roles/{roleCode}
    public Boolean deleteRole(String departmentCode, String roleCode) throws ApiException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (roleCode == null) {
            throw new ApiException(400, "Missing required parameter 'roleCode'");
        }
        String path = "/api/departments/{departmentCode}/roles/{roleCode}".replace("{departmentCode}", departmentCode).replace("{roleCode}", roleCode);
        return apiClient.invokeAPI(path, "DELETE", null, null, null, Boolean.class);
    }

    // GET /api/departments/{departmentCode}/roles/{roleCode}/permissions
    public List<PermissionDTO> getPermissionsByRole(String departmentCode, String roleCode) throws ApiException, IOException, InterruptedException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (roleCode == null) {
            throw new ApiException(400, "Missing required parameter 'roleCode'");
        }
        String path = "/api/departments/{departmentCode}/roles/{roleCode}/permissions".replace("{departmentCode}", departmentCode).replace("{roleCode}", roleCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    // GET /api/departments/{departmentCode}/roles/{roleCode}/children
    public RoleChildHierarchyDTO getRoleChildren(String departmentCode, String roleCode, Integer level) throws ApiException, IOException, InterruptedException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (roleCode == null) {
            throw new ApiException(400, "Missing required parameter 'roleCode'");
        }

        Map<String, String> queryParams = new HashMap<>();

        if (level != null) queryParams.put("level", level.toString());

        String path = "/api/departments/{departmentCode}/roles/{roleCode}/children".replace("{departmentCode}", departmentCode).replace("{roleCode}", roleCode);

        return apiClient.invokeAPI(path, "GET", queryParams, null, null, RoleChildHierarchyDTO.class);
    }

    // GET /api/departments/{departmentCode}/roles/{roleCode}/parents
    public RoleParentHierarchyDTO getRoleParents(String departmentCode, String roleCode, Integer level) throws ApiException, IOException, InterruptedException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (roleCode == null) {
            throw new ApiException(400, "Missing required parameter 'roleCode'");
        }

        Map<String, String> queryParams = new HashMap<>();

        if (level != null) queryParams.put("level", level.toString());

        String path = "/api/departments/{departmentCode}/roles/{roleCode}/parents".replace("{departmentCode}", departmentCode).replace("{roleCode}", roleCode);

        return apiClient.invokeAPI(path, "GET", queryParams, null, null, RoleParentHierarchyDTO.class);
    }



    // POST /api/departments/{departmentCode}/roles/{roleCode}/permissions
    public String addPermissionsToRole(String departmentCode, String roleCode, List<String> permissionCodes) throws ApiException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (roleCode == null) {
            throw new ApiException(400, "Missing required parameter 'roleCode'");
        }
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            throw new ApiException(400, "Missing required parameter 'permissionCodes'");
        }
        String path = "/api/departments/{departmentCode}/roles/{roleCode}/permissions".replace("{departmentCode}", departmentCode).replace("{roleCode}", roleCode);
        return apiClient.invokeAPI(path, "POST", null, permissionCodes, null, String.class);
    }

    // DELETE /api/departments/{departmentCode}/roles/{roleCode}/permissions
    public String removePermissionsFromRole(String departmentCode, String roleCode, List<String> permissionCodes) throws ApiException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (roleCode == null) {
            throw new ApiException(400, "Missing required parameter 'roleCode'");
        }
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            throw new ApiException(400, "Missing required parameter 'permissionCodes'");
        }
        String path = "/api/departments/{departmentCode}/roles/{roleCode}/permissions".replace("{departmentCode}", departmentCode).replace("{roleCode}", roleCode);
        return apiClient.invokeAPI(path, "DELETE", null, permissionCodes, null, String.class);
    }

    // GET /api/departments/{departmentCode}/roles/{roleCode}/permissions/available
    public List<PermissionDTO> getAvailablePermissionsForRole(String departmentCode, String roleCode) throws ApiException, IOException, InterruptedException {
        if (departmentCode == null) {
            throw new ApiException(400, "Missing required parameter 'departmentCode'");
        }
        if (roleCode == null) {
            throw new ApiException(400, "Missing required parameter 'roleCode'");
        }
        String path = "/api/departments/{departmentCode}/roles/{roleCode}/permissions/available".replace("{departmentCode}", departmentCode).replace("{roleCode}", roleCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }
}