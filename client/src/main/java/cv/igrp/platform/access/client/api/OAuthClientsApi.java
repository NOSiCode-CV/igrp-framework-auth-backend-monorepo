package cv.igrp.platform.access.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.JsonUtil;
import cv.igrp.platform.access.client.model.OAuthClientDTO;
import cv.igrp.platform.access.client.model.OAuthClientRequestDTO;

import java.io.IOException;
import java.util.List;

/**
 * Client for the iGRP authorization server's OAuth2 client management API
 * ({@code /api/clients}).
 *
 * <p>The raw {@code clientSecret} is returned only on
 * {@link #createOAuthClient(OAuthClientRequestDTO)} and must be captured by
 * the caller — subsequent reads do not include it.
 */
public class OAuthClientsApi {

    private IApiClient apiClient;

    public OAuthClientsApi() {
        this(new ApiClient());
    }

    public OAuthClientsApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /** List all OAuth2 clients. */
    public List<OAuthClientDTO> listOAuthClients() throws ApiException, IOException, InterruptedException {
        String response = apiClient.invokeAPIRaw("/api/clients", "GET", null, null, null);
        return JsonUtil.deserialize(response, new TypeReference<>() {});
    }

    /** Get a single OAuth2 client by id. */
    public OAuthClientDTO getOAuthClient(String id) throws ApiException {
        if (id == null) {
            throw new ApiException(400, "Missing the required parameter 'id'");
        }
        String path = "/api/clients/{id}".replaceAll("\\{" + "id" + "}", id);
        return apiClient.invokeAPI(path, "GET", null, null, null, OAuthClientDTO.class);
    }

    /**
     * Create a new OAuth2 client.
     *
     * <p>The returned DTO carries the generated raw {@code clientSecret} —
     * capture it before the response goes out of scope.
     */
    public OAuthClientDTO createOAuthClient(OAuthClientRequestDTO request) throws ApiException {
        if (request == null) {
            throw new ApiException(400, "Missing the required parameter 'request'");
        }
        return apiClient.invokeAPI("/api/clients", "POST", null, request, null, OAuthClientDTO.class);
    }

    /** Update an existing OAuth2 client. */
    public OAuthClientDTO updateOAuthClient(String id, OAuthClientRequestDTO request) throws ApiException {
        if (id == null || request == null) {
            throw new ApiException(400, "Missing required parameters");
        }
        String path = "/api/clients/{id}".replaceAll("\\{" + "id" + "}", id);
        return apiClient.invokeAPI(path, "PUT", null, request, null, OAuthClientDTO.class);
    }

    /** Delete an OAuth2 client. */
    public void deleteOAuthClient(String id) throws ApiException {
        if (id == null) {
            throw new ApiException(400, "Missing the required parameter 'id'");
        }
        String path = "/api/clients/{id}".replaceAll("\\{" + "id" + "}", id);
        apiClient.invokeAPI(path, "DELETE", null, null, null, Void.class);
    }
}
