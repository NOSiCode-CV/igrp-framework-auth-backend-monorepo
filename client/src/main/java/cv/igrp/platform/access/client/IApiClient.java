package cv.igrp.platform.access.client;

import cv.igrp.platform.access.client.auth.ClientCredentialsTokenProvider;

import java.io.IOException;
import java.util.Map;

// [marcelo.monteiro - 2025-06-26 14:54:23] TODO: Move this to a core library for API clients SDKs
public interface IApiClient {
    <T> T invokeAPI(String path,
                    String method,
                    Map<String, String> queryParams,
                    Object body,
                    Map<String, String> headerParams,
                    Class<T> returnType) throws ApiException;

    String invokeAPIRaw(String path,
                        String method,
                        Map<String, String> queryParams,
                        Object body,
                        Map<String, String> headerParams) throws ApiException, IOException, InterruptedException;

    String getToken();

    /**
     * @return the OAuth2 {@code client_credentials} token provider configured
     *         on this client (typically via
     *         {@code ApiClient.setClientCredentials(...)}), or {@code null}
     *         when no client credentials are configured. M2M-aware API
     *         classes use this to fetch a Bearer token transparently.
     */
    default ClientCredentialsTokenProvider getClientCredentialsTokenProvider() {
        return null;
    }
}
