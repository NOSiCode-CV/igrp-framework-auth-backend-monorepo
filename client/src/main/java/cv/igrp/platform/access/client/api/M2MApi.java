package cv.igrp.platform.access.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.JsonUtil;
import cv.igrp.platform.access.client.auth.ClientCredentialsTokenProvider;
import cv.igrp.platform.access.client.model.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Wrapper around the Access Management API's machine-to-machine endpoints
 * ({@code /api/m2m/**}).
 *
 * <p><b>Authentication.</b> Every M2M call must be authenticated as an
 * OAuth2 client. The token is obtained transparently from the API's
 * {@code POST /oauth2/token} endpoint using the {@code client_credentials}
 * grant — cached and refreshed by {@link ClientCredentialsTokenProvider}.
 *
 * <p>Configure the credentials <i>once</i> on the {@link ApiClient}:
 * <pre>{@code
 * ApiClient client = new ApiClient();
 * client.setBaseUrl("https://api.example.com/igrp-access-management");
 * client.setClientCredentials("my-service", "my-service-secret");
 *
 * M2MApi m2m = new M2MApi(client);
 * m2m.syncResources(resource, "my-service");
 * m2m.syncPermissions(permissions, "my-service");
 * }</pre>
 * The client library handles the token lifecycle. Pre-existing overloads
 * that accept a raw {@code m2mToken} are retained as {@link Deprecated} to
 * keep the API source-compatible — new callers should use the
 * client-credentials-aware variants.
 */
public class M2MApi {

    /** HTTP header carrying the calling-service identifier (informational). */
    private static final String SERVICE_HEADER = "X-Machine-Service-ID";

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

    // ─────────────────────────────────────────────────────────────────────
    //  New, client-credentials-aware signatures
    // ─────────────────────────────────────────────────────────────────────

    public Object syncPermissions(List<PermissionDTO> permissions, String serviceName)
            throws ApiException, IOException, InterruptedException {
        return apiClient.invokeAPIRaw("/api/m2m/sync/permissions", "POST", null,
                Objects.nonNull(permissions) ? permissions : List.of(),
                m2mHeaders(serviceName));
    }

    public Object syncResources(ResourceDTO resource, String serviceName)
            throws ApiException, IOException, InterruptedException {
        return apiClient.invokeAPIRaw("/api/m2m/sync/resources", "POST", null, resource,
                m2mHeaders(serviceName));
    }

    public Object syncApplication(ApplicationDTO application, String serviceName)
            throws ApiException, IOException, InterruptedException {
        return apiClient.invokeAPIRaw("/api/m2m/sync/applications", "POST", null, application,
                m2mHeaders(serviceName));
    }

    public Object syncApplicationMenus(String applicationCode, List<MenuEntryDTO> menus, String serviceName)
            throws ApiException, IOException, InterruptedException {
        return syncApplicationMenus(applicationCode, menus, (Boolean) null, serviceName);
    }

    /**
     * Sync application's menus.
     *
     * @param applicationCode target application code
     * @param menus           the menu entries to sync
     * @param syncRoles       when {@code false}, role associations are NOT synchronized
     *                        (existing role bindings on each menu are preserved untouched).
     *                        Pass {@code null} to use the server default ({@code true}).
     * @param serviceName     machine-to-machine service identifier (informational)
     */
    public Object syncApplicationMenus(String applicationCode, List<MenuEntryDTO> menus, Boolean syncRoles, String serviceName)
            throws ApiException, IOException, InterruptedException {
        Map<String, String> queryParams = null;
        if (syncRoles != null) {
            queryParams = new HashMap<>();
            queryParams.put("syncRoles", syncRoles.toString());
        }
        return apiClient.invokeAPIRaw("/api/m2m/sync/applications/%s/menus".formatted(applicationCode), "POST",
                queryParams, menus, m2mHeaders(serviceName));
    }

    public List<IGRPUserDTO> getUsers(boolean activeOnly, String applicationCode, String departmentCode,
                                      String roleCode, String permissionName,
                                      boolean includeChildrenDepartments, boolean includeChildrenRoles,
                                      List<String> identifiers, String serviceName)
            throws ApiException, IOException, InterruptedException {
        Map<String, String> queryParams = new HashMap<>();
        if (activeOnly) queryParams.put("activeOnly", "true");
        if (applicationCode != null) queryParams.put("applicationCode", applicationCode);
        if (departmentCode != null) queryParams.put("departmentCode", departmentCode);
        if (roleCode != null) queryParams.put("roleCode", roleCode);
        if (permissionName != null) queryParams.put("permissionName", permissionName);
        if (includeChildrenDepartments) queryParams.put("includeChildrenDepartments", "true");
        if (includeChildrenRoles) queryParams.put("includeChildrenRoles", "true");
        String response = apiClient.invokeAPIRaw("/api/m2m/users", "POST", queryParams,
                Objects.nonNull(identifiers) ? identifiers : List.of(),
                m2mHeaders(serviceName));
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<DepartmentDTO> getDepartments(boolean activeOnly, String parentCode,
                                              boolean includeChildrenDepartments,
                                              List<String> identifiers, String serviceName)
            throws IOException, InterruptedException, ApiException {
        Map<String, String> queryParams = new HashMap<>();
        if (activeOnly) queryParams.put("activeOnly", "true");
        if (parentCode != null) queryParams.put("parentCode", parentCode);
        if (includeChildrenDepartments) queryParams.put("includeChildrenDepartments", "true");
        String response = apiClient.invokeAPIRaw("/api/m2m/departments", "POST", queryParams,
                Objects.nonNull(identifiers) ? identifiers : List.of(),
                m2mHeaders(serviceName));
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    public List<RoleDTO> getRoles(boolean activeOnly, String parentCode, boolean includeChildrenRoles,
                                  List<String> identifiers, String serviceName)
            throws IOException, InterruptedException, ApiException {
        Map<String, String> queryParams = new HashMap<>();
        if (activeOnly) queryParams.put("activeOnly", "true");
        if (parentCode != null) queryParams.put("parentCode", parentCode);
        if (includeChildrenRoles) queryParams.put("includeChildrenRoles", "true");
        String response = apiClient.invokeAPIRaw("/api/m2m/roles", "POST", queryParams,
                Objects.nonNull(identifiers) ? identifiers : List.of(),
                m2mHeaders(serviceName));
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    /**
     * Build the headers map for an M2M call:
     * <ul>
     *   <li>{@code X-Machine-Service-ID} — purely informational; helps the
     *       API log which service made the call.</li>
     *   <li>{@code Authorization: Bearer <token>} — set automatically by
     *       {@link ApiClient} when client credentials are configured (see
     *       {@code ApiClient.setClientCredentials(...)}). If the credentials
     *       are missing the call will hit the server unauthenticated and
     *       the server will reject it with 401.</li>
     * </ul>
     *
     * <p>The token is fetched at most once per cache TTL and re-used across
     * every call here, removing the need for callers to manage tokens
     * manually.
     */
    private Map<String, String> m2mHeaders(String serviceName) throws ApiException {
        Map<String, String> headers = new HashMap<>();
        if (serviceName != null) {
            headers.put(SERVICE_HEADER, serviceName);
        }
        ClientCredentialsTokenProvider tokenProvider = apiClient.getClientCredentialsTokenProvider();
        if (tokenProvider != null) {
            headers.put("Authorization", "Bearer " + tokenProvider.getAccessToken());
        }
        return headers;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Legacy signatures — accept a raw token directly. Retained for
    //  callers that haven't migrated to client-credentials yet. New code
    //  should configure ApiClient.setClientCredentials(...) and use the
    //  variants above.
    // ─────────────────────────────────────────────────────────────────────

    @Deprecated
    public Object syncPermissions(List<PermissionDTO> permissions, String m2mToken, String serviceName)
            throws ApiException, IOException, InterruptedException {
        return apiClient.invokeAPIRaw("/api/m2m/sync/permissions", "POST", null,
                Objects.nonNull(permissions) ? permissions : List.of(),
                legacyHeaders(m2mToken, serviceName));
    }

    @Deprecated
    public Object syncResources(ResourceDTO resource, String m2mToken, String serviceName)
            throws ApiException, IOException, InterruptedException {
        return apiClient.invokeAPIRaw("/api/m2m/sync/resources", "POST", null, resource,
                legacyHeaders(m2mToken, serviceName));
    }

    @Deprecated
    public Object syncApplication(ApplicationDTO application, String m2mToken, String serviceName)
            throws ApiException, IOException, InterruptedException {
        return apiClient.invokeAPIRaw("/api/m2m/sync/applications", "POST", null, application,
                legacyHeaders(m2mToken, serviceName));
    }

    @Deprecated
    public Object syncApplicationMenus(String applicationCode, List<MenuEntryDTO> menus,
                                       String m2mToken, String serviceName)
            throws ApiException, IOException, InterruptedException {
        return syncApplicationMenus(applicationCode, menus, (Boolean) null, m2mToken, serviceName);
    }

    @Deprecated
    public Object syncApplicationMenus(String applicationCode, List<MenuEntryDTO> menus, Boolean syncRoles,
                                       String m2mToken, String serviceName)
            throws ApiException, IOException, InterruptedException {
        Map<String, String> queryParams = null;
        if (syncRoles != null) {
            queryParams = new HashMap<>();
            queryParams.put("syncRoles", syncRoles.toString());
        }
        return apiClient.invokeAPIRaw("/api/m2m/sync/applications/%s/menus".formatted(applicationCode), "POST",
                queryParams, menus, legacyHeaders(m2mToken, serviceName));
    }

    @Deprecated
    public List<IGRPUserDTO> getUsers(boolean activeOnly, String applicationCode, String departmentCode,
                                      String roleCode, String permissionName,
                                      boolean includeChildrenDepartments, boolean includeChildrenRoles,
                                      List<String> identifiers, String m2mToken, String serviceName)
            throws ApiException, IOException, InterruptedException {
        Map<String, String> queryParams = new HashMap<>();
        if (activeOnly) queryParams.put("activeOnly", "true");
        if (applicationCode != null) queryParams.put("applicationCode", applicationCode);
        if (departmentCode != null) queryParams.put("departmentCode", departmentCode);
        if (roleCode != null) queryParams.put("roleCode", roleCode);
        if (permissionName != null) queryParams.put("permissionName", permissionName);
        if (includeChildrenDepartments) queryParams.put("includeChildrenDepartments", "true");
        if (includeChildrenRoles) queryParams.put("includeChildrenRoles", "true");
        String response = apiClient.invokeAPIRaw("/api/m2m/users", "POST", queryParams,
                Objects.nonNull(identifiers) ? identifiers : List.of(),
                legacyHeaders(m2mToken, serviceName));
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    @Deprecated
    public List<DepartmentDTO> getDepartments(boolean activeOnly, String parentCode,
                                              boolean includeChildrenDepartments,
                                              List<String> identifiers, String m2mToken, String serviceName)
            throws IOException, InterruptedException, ApiException {
        Map<String, String> queryParams = new HashMap<>();
        if (activeOnly) queryParams.put("activeOnly", "true");
        if (parentCode != null) queryParams.put("parentCode", parentCode);
        if (includeChildrenDepartments) queryParams.put("includeChildrenDepartments", "true");
        String response = apiClient.invokeAPIRaw("/api/m2m/departments", "POST", queryParams,
                Objects.nonNull(identifiers) ? identifiers : List.of(),
                legacyHeaders(m2mToken, serviceName));
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    @Deprecated
    public List<RoleDTO> getRoles(boolean activeOnly, String parentCode, boolean includeChildrenRoles,
                                  List<String> identifiers, String m2mToken, String serviceName)
            throws IOException, InterruptedException, ApiException {
        Map<String, String> queryParams = new HashMap<>();
        if (activeOnly) queryParams.put("activeOnly", "true");
        if (parentCode != null) queryParams.put("parentCode", parentCode);
        if (includeChildrenRoles) queryParams.put("includeChildrenRoles", "true");
        String response = apiClient.invokeAPIRaw("/api/m2m/roles", "POST", queryParams,
                Objects.nonNull(identifiers) ? identifiers : List.of(),
                legacyHeaders(m2mToken, serviceName));
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    private Map<String, String> legacyHeaders(String m2mToken, String serviceName) {
        Map<String, String> headers = new HashMap<>();
        if (serviceName != null) headers.put(SERVICE_HEADER, serviceName);
        if (m2mToken != null) headers.put("X-Machine-Auth-Token", m2mToken);
        return headers;
    }
}
