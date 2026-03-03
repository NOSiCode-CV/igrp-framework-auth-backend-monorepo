package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.JsonUtil;
import cv.igrp.platform.access.client.model.ApplicationDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import cv.igrp.platform.access.client.model.MenuEntryDTO;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationsApi {
    private IApiClient apiClient;

    public ApplicationsApi() {
        this(new ApiClient());
    }

    public ApplicationsApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // Get application by ID
    public ApplicationDTO getApplicationById(Integer id) throws ApiException {
        if (id == null) {
            throw new ApiException(400, "Missing the required parameter 'id'");
        }

        String path = "/api/applications/{id}"
                .replaceAll("\\{" + "id" + "}", id.toString());

        return apiClient.invokeAPI(
                path,
                "GET",
                null,
                null,
                null,
                ApplicationDTO.class
        );
    }

    // Get application by code
    public ApplicationDTO getApplicationByCode(String code) throws ApiException {
        if (code == null) {
            throw new ApiException(400, "Missing the required parameter 'code'");
        }

        String path = "/api/applications/by-code/{code}"
                .replaceAll("\\{" + "code" + "}", code);

        return apiClient.invokeAPI(
                path,
                "GET",
                null,
                null,
                null,
                ApplicationDTO.class
        );
    }

    // Update application
    public ApplicationDTO updateApplication(String code, ApplicationDTO applicationDTO) throws ApiException {
        if (code == null || applicationDTO == null) {
            throw new ApiException(400, "Missing required parameters");
        }

        String path = "/api/applications/{code}".replace("{code}", code);

        return apiClient.invokeAPI(
                path,
                "PUT",
                null,
                applicationDTO,
                null,
                ApplicationDTO.class
        );
    }

    // Delete application
    public String deleteApplication(String code) throws ApiException {
        if (code == null) {
            throw new ApiException(400, "Missing the required parameter 'code'");
        }

        String path = "/api/applications/{code}".replace("{code}", code);

        return apiClient.invokeAPI(
                path,
                "DELETE",
                null,
                null,
                null,
                String.class
        );
    }

    // Get all applications with filters
    public List<ApplicationDTO> getApplications(String code, String name, String slug, String departmentCode, String type) throws ApiException, IOException, InterruptedException {
        Map<String, String> queryParams = new HashMap<>();
        if (code != null) queryParams.put("code", code);
        if (name != null) queryParams.put("name", name);
        if (slug != null) queryParams.put("slug", slug);
        if (departmentCode != null) queryParams.put("departmentCode", departmentCode);
        if (type != null) queryParams.put("type", type);

        String response = apiClient.invokeAPIRaw(
                "/api/applications",
                "GET",
                queryParams,
                null,
                null
        );

        return JsonUtil.deserialize(response, new TypeReference<>() {
        });
    }

    // Create new application
    public ApplicationDTO createApplication(ApplicationDTO applicationDTO) throws ApiException {
        if (applicationDTO == null) {
            throw new ApiException(400, "Missing the required parameter 'applicationDTO'");
        }

        return apiClient.invokeAPI(
                "/api/applications",
                "POST",
                null,
                applicationDTO,
                null,
                ApplicationDTO.class
        );
    }

    // Get application custom fields
    public String getApplicationCustomFields(String code) throws ApiException {
        if (code == null) {
            throw new ApiException(400, "Missing the required parameter 'code'");
        }

        String path = "/api/applications/{code}/custom-fields".replace("{code}", code);

        return apiClient.invokeAPI(
                path,
                "GET",
                null,
                null,
                null,
                String.class
        );
    }

    // Add custom fields to application
    public String addApplicationCustomFields(String code, Map<String, Object> customFields) throws ApiException {
        if (code == null || customFields == null) {
            throw new ApiException(400, "Missing required parameters");
        }

        String path = "/api/applications/{code}/custom-fields".replace("{code}", code);

        return apiClient.invokeAPI(
                path,
                "POST",
                null,
                customFields,
                null,
                String.class
        );
    }

    // Remove custom fields from application
    public String removeApplicationCustomFields(String code, List<String> keys) throws ApiException {
        if (code == null || keys == null) {
            throw new ApiException(400, "Missing required parameters");
        }

        String path = "/api/applications/{code}/custom-fields".replace("{code}", code);

        return apiClient.invokeAPI(
                path,
                "DELETE",
                null,
                keys,
                null,
                String.class
        );
    }

    // Get applications by IDs
    public List<ApplicationDTO> getApplicationsByIds(List<Integer> ids) throws ApiException, IOException, InterruptedException {
        if (ids == null || ids.isEmpty()) {
            throw new ApiException(400, "Missing required IDs list");
        }

        String response = apiClient.invokeAPIRaw(
                "/api/applications/by-ids",
                "POST",
                null,
                ids,
                null
        );

        return JsonUtil.deserialize(response, new TypeReference<>() {
        });
    }

    public List<MenuEntryDTO> getMenus(String appCode) throws ApiException, IOException, InterruptedException {
        if (appCode == null || appCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'appCode'");
        }

        String path = "/api/applications/{appCode}/menus"
                .replaceAll("\\{" + "appCode" + "}", appCode);

        String response = apiClient.invokeAPIRaw(
                path,
                "GET",
                null,
                null,
                null
        );

        return JsonUtil.deserialize(response, new TypeReference<>() {
        });
    }

    public MenuEntryDTO createMenu(String appCode, MenuEntryDTO menuEntryDTO) throws ApiException, IOException, InterruptedException {
        if (appCode == null || appCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'appCode'");
        }
        if (menuEntryDTO == null) {
            throw new ApiException(400, "Missing the required parameter 'menuEntryDTO'");
        }

        String path = "/api/applications/{appCode}/menus"
                .replaceAll("\\{" + "appCode" + "}", appCode);

        String response = apiClient.invokeAPIRaw(
                path,
                "POST",
                null,
                menuEntryDTO,
                null
        );

        return JsonUtil.deserialize(response, new TypeReference<>() {
        });
    }

    public MenuEntryDTO updateMenu(String appCode, String menuCode, MenuEntryDTO menuEntryDTO) throws ApiException, IOException, InterruptedException {
        if (appCode == null || appCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'appCode'");
        }
        if (menuCode == null || menuCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'menuCode'");
        }
        if (menuEntryDTO == null) {
            throw new ApiException(400, "Missing the required parameter 'menuEntryDTO'");
        }

        String path = "/api/applications/{appCode}/menus/{menuCode}"
                .replaceAll("\\{" + "appCode" + "}", appCode)
                .replaceAll("\\{" + "menuCode" + "}", menuCode);

        String response = apiClient.invokeAPIRaw(
                path,
                "PUT",
                null,
                menuEntryDTO,
                null
        );

        return JsonUtil.deserialize(response, new TypeReference<>() {
        });
    }

    public String deleteMenu(String appCode, String menuCode) throws ApiException {
        if (appCode == null || appCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'appCode'");
        }
        if (menuCode == null || menuCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'menuCode'");
        }

        String path = "/api/applications/{appCode}/menus/{menuCode}"
                .replaceAll("\\{" + "appCode" + "}", appCode)
                .replaceAll("\\{" + "menuCode" + "}", menuCode);

        return apiClient.invokeAPI(
                path,
                "DELETE",
                null,
                null,
                null,
                String.class
        );
    }

    public MenuEntryDTO addRolesToMenu(String appCode, String menuCode, String departmentCode, List<String> roleNames) throws ApiException {
        if (appCode == null || appCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'appCode'");
        }
        if (menuCode == null || menuCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'menuCode'");
        }
        if (departmentCode == null || departmentCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'departmentCode'");
        }
        if (roleNames == null) {
            throw new ApiException(400, "Missing the required parameter 'roleNames'");
        }

        String path = "/api/applications/{appCode}/menus/{menuCode}/roles?departmentCode={departmentCode}"
                .replaceAll("\\{" + "appCode" + "}", appCode)
                .replaceAll("\\{" + "menuCode" + "}", menuCode)
                .replaceAll("\\{" + "departmentCode" + "}", departmentCode);

        return apiClient.invokeAPI(
                path,
                "POST",
                null,
                roleNames,
                null,
                MenuEntryDTO.class
        );
    }

    public MenuEntryDTO removeRolesFromMenu(String appCode, String menuCode, String departmentCode, List<String> roleNames) throws ApiException {

        if (appCode == null || appCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'appCode'");
        }
        if (menuCode == null || menuCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'menuCode'");
        }
        if (departmentCode == null || departmentCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'departmentCode'");
        }
        if (roleNames == null) {
            throw new ApiException(400, "Missing the required parameter 'roleNames'");
        }

        String path = "/api/applications/{appCode}/menus/{menuCode}/roles?departmentCode={departmentCode}"
                .replaceAll("\\{" + "appCode" + "}", appCode)
                .replaceAll("\\{" + "menuCode" + "}", menuCode)
                .replaceAll("\\{" + "departmentCode" + "}", departmentCode);

        return apiClient.invokeAPI(
                path,
                "DELETE",
                null,
                roleNames,
                null,
                MenuEntryDTO.class
        );

    }

    public String linkResourceToApplication(String appCode, String resourceName) throws ApiException {
        if (appCode == null || appCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'appCode'");
        }
        if (resourceName == null || resourceName.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'resourceName'");
        }

        String path = "/api/applications/{appCode}/resources/{resourceName}"
                .replaceAll("\\{" + "appCode" + "}", appCode)
                .replaceAll("\\{" + "resourceName" + "}", resourceName);

        return apiClient.invokeAPI(
                path,
                "POST",
                null,
                null,
                null,
                String.class
        );
    }

    public String unlinkResourceFromApplication(String appCode, String resourceName) throws ApiException {
        if (appCode == null || appCode.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'appCode'");
        }
        if (resourceName == null || resourceName.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter 'resourceName'");
        }

        String path = "/api/applications/{appCode}/resources/{resourceName}"
                .replaceAll("\\{" + "appCode" + "}", appCode)
                .replaceAll("\\{" + "resourceName" + "}", resourceName);

        return apiClient.invokeAPI(
                path,
                "DELETE",
                null,
                null,
                null,
                String.class
        );
    }

}