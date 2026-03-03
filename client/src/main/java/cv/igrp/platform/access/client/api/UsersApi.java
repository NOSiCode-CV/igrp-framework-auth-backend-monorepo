package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.*;
import cv.igrp.platform.access.client.model.*;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class UsersApi {
    private IApiClient apiClient;

    public UsersApi() {
        this(new ApiClient());
    }

    public UsersApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // Existing username-based endpoints (kept for backward compatibility)
    public IGRPUserDTO getUser(String username) throws ApiException {
        if (username == null) {
            throw new ApiException(400, "Missing required parameter 'username'");
        }
        String path = "/api/users/{username}".replaceAll("\\{" + "username" + "}", username);
        return apiClient.invokeAPI(path, "GET", null, null, null, IGRPUserDTO.class);
    }

    public IGRPUserDTO updateUser(String username, IGRPUserDTO userDTO) throws ApiException {
        if (username == null || userDTO == null) {
            throw new ApiException(400, "Missing required parameters");
        }
        String path = "/api/users/{username}".replaceAll("\\{" + "username" + "}", username);
        return apiClient.invokeAPI(path, "PUT", null, userDTO, null, IGRPUserDTO.class);
    }

    public IGRPUserDTO createUser(IGRPUserDTO userDTO) throws ApiException {
        if (userDTO == null) {
            throw new ApiException(400, "Missing required parameter 'userDTO'");
        }
        return apiClient.invokeAPI("/api/users", "POST", null, userDTO, null, IGRPUserDTO.class);
    }

    public IGRPUserDTO inviteUser(IGRPUserDTO userDTO) throws ApiException {
        if (userDTO == null) {
            throw new ApiException(400, "Missing required parameter 'userDTO'");
        }
        return apiClient.invokeAPI("/api/users/invite", "POST", null, userDTO, null, IGRPUserDTO.class);
    }

    public RoleDTO addRolesToUser(String username, List<String> roleNames) throws ApiException {
        if (username == null || roleNames == null) {
            throw new ApiException(400, "Missing required parameters");
        }
        String path = "/api/users/{username}/roles".replaceAll("\\{" + "username" + "}", username);
        return apiClient.invokeAPI(path, "POST", null, roleNames, null, RoleDTO.class);
    }

    public RoleDTO removeRolesFromUser(String username, List<String> roleNames) throws ApiException {
        if (username == null || roleNames == null) {
            throw new ApiException(400, "Missing required parameters");
        }
        String path = "/api/users/{username}/roles".replaceAll("\\{" + "username" + "}", username);
        return apiClient.invokeAPI(path, "DELETE", null, roleNames, null, RoleDTO.class);
    }

    public List<RoleDTO> getUserRoles(String username) throws ApiException, IOException, InterruptedException {
        if (username == null) {
            throw new ApiException(400, "Missing required parameter: 'username'");
        }
        String path = "/api/users/{username}/roles".replaceAll("\\{" + "username" + "}", username);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<IGRPUserDTO> getUsers(String applicationCode, String departmentCode, String name, String email) throws ApiException, IOException, InterruptedException {
        Map<String, String> queryParams = new java.util.HashMap<>();
        if (applicationCode != null) queryParams.put("applicationCode", applicationCode);
        if (departmentCode != null) queryParams.put("departmentCode", departmentCode);
        if (name != null) queryParams.put("name", name);
        if (email != null) queryParams.put("email", email);
        String response = apiClient.invokeAPIRaw("/api/users", "GET", queryParams, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    // ID-based endpoints
    public IGRPUserDTO getUser(Integer id) throws ApiException {
        if (id == null) throw new ApiException(400, "Missing required parameter 'id'");
        String path = "/api/users/{id}".replace("{id}", id.toString());
        return apiClient.invokeAPI(path, "GET", null, null, null, IGRPUserDTO.class);
    }

    public IGRPUserDTO updateUser(Integer id, IGRPUserDTO user) throws ApiException {
        if (id == null || user == null) throw new ApiException(400, "Missing required parameters");
        String path = "/api/users/{id}".replace("{id}", id.toString());
        return apiClient.invokeAPI(path, "PUT", null, user, null, IGRPUserDTO.class);
    }

    public IGRPUserDTO updateUserStatus(Integer id, String value) throws ApiException {
        if (id == null || value == null) throw new ApiException(400, "Missing required parameters");
        String path = "/api/users/{id}/status".replace("{id}", id.toString());
        Map<String, String> query = new HashMap<>();
        query.put("value", value);
        return apiClient.invokeAPI(path + "?value=" + value, "PUT", null, null, null, IGRPUserDTO.class);
    }

    // Invitations
    public InvitationDTO inviteUser(InviteUserDTO invite) throws ApiException {
        if (invite == null) throw new ApiException(400, "Missing required parameter 'invite'");
        return apiClient.invokeAPI("/api/users/invite", "POST", null, invite, null, InvitationDTO.class);
    }

    public InvitationDTO respondUserInvitation(UserInvitationResponseDTO responseBody, String token) throws ApiException {
        if (token == null || responseBody == null) throw new ApiException(400, "Missing required parameters");
        String path = "/api/users/invite/response";
        return apiClient.invokeAPI(path + "?token=" + token, "POST", null, responseBody, null, InvitationDTO.class);
    }

    public InvitationDTO resendUserInvitation(Integer id) throws ApiException {
        if (id == null) throw new ApiException(400, "Missing required parameter 'id'");
        String path = "/api/users/invite/{id}".replace("{id}", id.toString());
        return apiClient.invokeAPI(path, "PUT", null, null, null, InvitationDTO.class);
    }

    public InvitationDTO cancelUserInvitation(Integer id) throws ApiException {
        if (id == null) throw new ApiException(400, "Missing required parameter 'id'");
        String path = "/api/users/invite/{id}".replace("{id}", id.toString());
        return apiClient.invokeAPI(path, "DELETE", null, null, null, InvitationDTO.class);
    }

    public InvitationDTO getUserInvitation(Integer id) throws ApiException {
        if (id == null) throw new ApiException(400, "Missing required parameter 'id'");
        String path = "/api/users/invite/{id}".replace("{id}", id.toString());
        return apiClient.invokeAPI(path, "GET", null, null, null, InvitationDTO.class);
    }

    public List<InvitationDTO> getUserInvitations(String email) throws ApiException, IOException, InterruptedException {
        Map<String, String> query = null;
        if (email != null) {
            query = new HashMap<>();
            query.put("email", email);
        }
        String response = apiClient.invokeAPIRaw("/api/users/invite", "GET", query, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    // Roles by user id & department
    public List<RoleDTO> getUserRoles(Integer id) throws ApiException, IOException, InterruptedException {
        if (id == null) throw new ApiException(400, "Missing required parameter 'id'");
        String path = "/api/users/{id}/roles".replace("{id}", id.toString());
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<PermissionDTO> getUserPermissions(Integer id) throws ApiException, IOException, InterruptedException {
        if (id == null) throw new ApiException(400, "Missing required parameter 'id'");
        String path = "/api/users/{id}/permissions".replace("{id}", id.toString());
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public RoleDTO addRolesToUser(Integer id, String departmentCode, List<String> roleCodes) throws ApiException {
        if (id == null || departmentCode == null || roleCodes == null) throw new ApiException(400, "Missing required parameters");
        String path = "/api/users/{id}/departments/{departmentCode}/roles"
                .replace("{id}", id.toString())
                .replace("{departmentCode}", departmentCode);
        return apiClient.invokeAPI(path, "POST", null, roleCodes, null, RoleDTO.class);
    }

    public RoleDTO removeRolesFromUser(Integer id, String departmentCode, List<String> roleCodes) throws ApiException {
        if (id == null || departmentCode == null || roleCodes == null) throw new ApiException(400, "Missing required parameters");
        String path = "/api/users/{id}/departments/{departmentCode}/roles"
                .replace("{id}", id.toString())
                .replace("{departmentCode}", departmentCode);
        return apiClient.invokeAPI(path, "DELETE", null, roleCodes, null, RoleDTO.class);
    }

    public IGRPUserDTO getCurrentUser() throws ApiException {
        return apiClient.invokeAPI("/api/users/me", "GET", null, null, null, IGRPUserDTO.class);
    }

    public RoleDepartmentDTO getCurrentUserActiveRole() throws ApiException {
        return apiClient.invokeAPI("/api/users/me/roles/active", "GET", null, null, null, RoleDepartmentDTO.class);
    }

    public RoleDepartmentDTO getUserActiveRole(Integer id) throws ApiException {
        if (id == null) throw new ApiException(400, "Missing required parameter 'id'");
        String path = "/api/users/{id}/roles/active".replace("{id}", id.toString());
        return apiClient.invokeAPI(path, "GET", null, null, null, RoleDepartmentDTO.class);
    }

    public RoleDepartmentDTO setUserActiveRole(RoleDepartmentDTO body, Integer id) throws ApiException {
        if (id == null || body == null) throw new ApiException(400, "Missing required parameters");
        String path = "/api/users/{id}/roles/active".replace("{id}", id.toString());
        return apiClient.invokeAPI(path, "POST", null, body, null, RoleDepartmentDTO.class);
    }

    public RoleDepartmentDTO setCurrentUserActiveRole(RoleDepartmentDTO body) throws ApiException {
        if (body == null) throw new ApiException(400, "Missing required parameters");
        String path = "/api/users/me/roles/active";
        return apiClient.invokeAPI(path, "POST", null, body, null, RoleDepartmentDTO.class);
    }

    public List<DepartmentDTO> getCurrentUserDepartments() throws ApiException, IOException, InterruptedException {
        String response = apiClient.invokeAPIRaw("/api/users/me/departments", "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<DepartmentDTO> getUserDepartments(Integer id) throws ApiException, IOException, InterruptedException {
        if (id == null) throw new ApiException(400, "Missing required parameter 'id'");
        String path = "/api/users/{id}/departments".replace("{id}", id.toString());
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<RoleUserDTO> getCurrentUserDepartmentRoles(String departmentCode) throws ApiException, IOException, InterruptedException {
        if (departmentCode == null) throw new ApiException(400, "Missing required parameter 'departmentCode'");
        String path = "/api/users/me/departments/{departmentCode}/roles".replace("{departmentCode}", departmentCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<RoleUserDTO> getUserDepartmentRoles(Integer id, String departmentCode) throws ApiException, IOException, InterruptedException {
        if (id == null || departmentCode == null) throw new ApiException(400, "Missing required parameters");
        String path = "/api/users/{id}/departments/{departmentCode}/roles"
                .replace("{id}", id.toString())
                .replace("{departmentCode}", departmentCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<RoleDTO> getCurrentUserRoles() throws ApiException, IOException, InterruptedException {
        String response = apiClient.invokeAPIRaw("/api/users/me/roles", "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<PermissionDTO> getCurrentUserPermissions(String roleCode) throws ApiException, IOException, InterruptedException {
        Map<String, String> query = null;
        if (roleCode != null) { query = new HashMap<>(); query.put("roleCode", roleCode); }
        String response = apiClient.invokeAPIRaw("/api/users/me/permissions", "GET", query, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<ApplicationDTO> getCurrentUserApplications(String applicationName) throws ApiException, IOException, InterruptedException {
        Map<String, String> query = null;
        if (applicationName != null) { query = new HashMap<>(); query.put("applicationName", applicationName); }
        String response = apiClient.invokeAPIRaw("/api/users/me/applications", "GET", query, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<ApplicationDTO> getCurrentUserRecentApplications(String applicationName, String max) throws ApiException, IOException, InterruptedException {
        Map<String, String> query = new HashMap<>();
        if (applicationName != null) query.put("applicationName", applicationName);
        if (max != null) query.put("max", max);
        String response = apiClient.invokeAPIRaw("/api/users/me/applications/recent", "GET", query.isEmpty()? null: query, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public ApplicationDTO registerCurrentUserApplicationAccess(String applicationCode) throws ApiException {
        if (applicationCode == null) throw new ApiException(400, "Missing required parameter 'applicationCode'");
        String path = "/api/users/me/applications/recent/{applicationCode}".replace("{applicationCode}", applicationCode);
        return apiClient.invokeAPI(path, "POST", null, null, null, ApplicationDTO.class);
    }

    public List<ApplicationDTO> getCurrentUserFavoriteApplications(String applicationName) throws ApiException, IOException, InterruptedException {
        Map<String, String> query = null;
        if (applicationName != null) { query = new HashMap<>(); query.put("applicationName", applicationName); }
        String response = apiClient.invokeAPIRaw("/api/users/me/applications/favorites", "GET", query, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public ApplicationDTO addCurrentUserFavoriteApplication(String applicationCode) throws ApiException {
        if (applicationCode == null) throw new ApiException(400, "Missing required parameter 'applicationCode'");
        String path = "/api/users/me/applications/favorites/{applicationCode}".replace("{applicationCode}", applicationCode);
        return apiClient.invokeAPI(path, "POST", null, null, null, ApplicationDTO.class);
    }

    public ApplicationDTO removeCurrentUserFavoriteApplication(String applicationCode) throws ApiException {
        if (applicationCode == null) throw new ApiException(400, "Missing required parameter 'applicationCode'");
        String path = "/api/users/me/applications/favorites/{applicationCode}".replace("{applicationCode}", applicationCode);
        return apiClient.invokeAPI(path, "DELETE", null, null, null, ApplicationDTO.class);
    }

    public List<ApplicationDTO> getUserApplications(Integer id) throws ApiException, IOException, InterruptedException {
        if (id == null) throw new ApiException(400, "Missing required parameter 'id'");
        String path = "/api/users/{id}/applications".replace("{id}", id.toString());
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<MenuEntryDTO> getCurrentUserApplicationMenus(String applicationCode) throws ApiException, IOException, InterruptedException {
        if (applicationCode == null) throw new ApiException(400, "Missing required parameter 'applicationCode'");
        String path = "/api/users/me/applications/{applicationCode}/menus".replace("{applicationCode}", applicationCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<MenuEntryDTO> getUserApplicationMenus(Integer id, String applicationCode) throws ApiException, IOException, InterruptedException {
        if (id == null || applicationCode == null) throw new ApiException(400, "Missing required parameters");
        String path = "/api/users/{id}/applications/{applicationCode}/menus"
                .replace("{id}", id.toString())
                .replace("{applicationCode}", applicationCode);
        String response = apiClient.invokeAPIRaw(path, "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public boolean isSuperadmin() {
        return JwtUtil.hasSuperAdminRole(apiClient.getToken());
    }

}