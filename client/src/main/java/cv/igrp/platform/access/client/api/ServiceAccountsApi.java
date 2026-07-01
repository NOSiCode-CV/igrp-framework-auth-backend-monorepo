package cv.igrp.platform.access.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.JsonUtil;
import cv.igrp.platform.access.client.model.ServiceAccountDTO;
import cv.igrp.platform.access.client.model.ServiceAccountRequestDTO;

import java.io.IOException;
import java.util.List;

/**
 * Client for the iGRP authorization server's service-account management API
 * ({@code /api/service-accounts}).
 *
 * <p>A service account is a 1:1 human-manageable wrapper around an OAuth2
 * client used for machine-to-machine access. The linked OAuth2 client (and
 * its secret) is NOT managed here — create it first via
 * {@link OAuthClientsApi#createOAuthClient(cv.igrp.platform.access.client.model.OAuthClientRequestDTO)}
 * (with {@code grantTypes} including {@code client_credentials}), then pass
 * its id as {@code ServiceAccountRequestDTO.oauthClientId} on
 * {@link #createServiceAccount(ServiceAccountRequestDTO)}.
 *
 * <p>PUT ({@link #updateServiceAccount(String, ServiceAccountRequestDTO)})
 * uses REPLACEMENT semantics on {@code roleIds} and {@code permissionIds} —
 * ids omitted from the request are unassigned. Effective permissions at
 * token-issue time are the UNION of role-inherited and directly-granted
 * permissions.
 */
public class ServiceAccountsApi {

    private IApiClient apiClient;

    public ServiceAccountsApi() {
        this(new ApiClient());
    }

    public ServiceAccountsApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /** List all service accounts. */
    public List<ServiceAccountDTO> listServiceAccounts() throws ApiException, IOException, InterruptedException {
        String response = apiClient.invokeAPIRaw("/api/service-accounts", "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    /**
     * Get a single service account by id, including its assigned role codes
     * and directly-granted permission names.
     */
    public ServiceAccountDTO getServiceAccount(String id) throws ApiException {
        if (id == null) {
            throw new ApiException(400, "Missing the required parameter 'id'");
        }
        String path = "/api/service-accounts/{id}".replaceAll("\\{" + "id" + "}", id);
        return apiClient.invokeAPI(path, "GET", null, null, null, ServiceAccountDTO.class);
    }

    /**
     * Create a new service account linked to an existing OAuth2 client.
     * The response does NOT include a secret — the secret was surfaced on
     * the OAuth2 client's creation response and is not recoverable afterwards.
     */
    public ServiceAccountDTO createServiceAccount(ServiceAccountRequestDTO request) throws ApiException {
        if (request == null) {
            throw new ApiException(400, "Missing the required parameter 'request'");
        }
        return apiClient.invokeAPI("/api/service-accounts", "POST", null, request, null, ServiceAccountDTO.class);
    }

    /**
     * Replace a service account's mutable fields (name, description, active
     * flag, application, roles, direct permissions).
     * {@code roleIds} and {@code permissionIds} use REPLACEMENT semantics —
     * ids omitted from the request are removed from the account.
     */
    public ServiceAccountDTO updateServiceAccount(String id, ServiceAccountRequestDTO request) throws ApiException {
        if (id == null || request == null) {
            throw new ApiException(400, "Missing required parameters");
        }
        String path = "/api/service-accounts/{id}".replaceAll("\\{" + "id" + "}", id);
        return apiClient.invokeAPI(path, "PUT", null, request, null, ServiceAccountDTO.class);
    }

    /**
     * Delete a service account. This does NOT delete the linked OAuth2
     * client — that must be removed separately via
     * {@link OAuthClientsApi#deleteOAuthClient(String)} if the client is
     * no longer needed.
     */
    public void deleteServiceAccount(String id) throws ApiException {
        if (id == null) {
            throw new ApiException(400, "Missing the required parameter 'id'");
        }
        String path = "/api/service-accounts/{id}".replaceAll("\\{" + "id" + "}", id);
        apiClient.invokeAPI(path, "DELETE", null, null, null, Void.class);
    }
}
