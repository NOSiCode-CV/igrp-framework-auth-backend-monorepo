package cv.igrp.framework.auth.permify.configuration;

import cv.igrp.framework.auth.core.authorization.service.AuthorizationCore;
import cv.igrp.framework.auth.permify.internal.PermifyAdapter;
import org.permify.ApiClient;
import org.permify.api.DataApi;
import org.permify.api.PermissionApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class PermifyConfiguration {

    @Value("${permify.server.url}")
    private String permifyServerUrl;
    @Value("${permify.api.key:}")
    private String permifyApiKey;

    @Bean
    public ApiClient permifyApiClient() {
        ApiClient client = new ApiClient();
        client.setBasePath(permifyServerUrl);

        if (permifyApiKey != null && !permifyApiKey.isEmpty()) {
            client.addDefaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + permifyApiKey);
        }

        client.addDefaultHeader("Content-Type", "application/json");
        return client;
    }

    @Bean
    public DataApi dataApi(ApiClient apiClient) {
        return new DataApi(apiClient);
    }

    @Bean
    public PermissionApi permissionApi(ApiClient apiClient) {
        return new PermissionApi(apiClient);
    }

    @Bean
    public AuthorizationCore permifyAdapter(PermissionApi permissionApi, DataApi dataApi) {
        return new PermifyAdapter(permissionApi, dataApi);
    }
}
