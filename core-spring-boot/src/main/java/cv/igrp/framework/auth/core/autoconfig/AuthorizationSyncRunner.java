package cv.igrp.framework.auth.core.autoconfig;

import cv.igrp.framework.auth.generated.PermissionsRegistry;
import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.api.M2MApi;
import cv.igrp.platform.access.client.constants.Status;
import cv.igrp.platform.access.client.model.PermissionDTO;
import cv.igrp.platform.access.client.model.ResourceDTO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Automatically synchronizes code-defined permissions with the Access Management API.
 */
@Component
public class AuthorizationSyncRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationSyncRunner.class);

    private final ApiClient accessClient;

    @Value("${igrp.access.m2m.sync-token:}")
    private String m2mToken;

    @Value("${spring.application.name:}")
    private String applicationName;

    public AuthorizationSyncRunner(ApiClient accessClient) {
        this.accessClient = accessClient;
    }

    @PostConstruct
    public void syncAuthorization() {
        try {
            LOGGER.info("[Authorization Sync] Starting authorization synchronization with Access Management API...");

            List<PermissionDTO> permissions = Arrays.stream(PermissionsRegistry.Permission.values())
                    .map(p -> {
                        var perm = new PermissionDTO();
                        perm.setName(p.getCode());
                        perm.setDescription(p.getDescription());
                        perm.setStatus(p.enabled() ? Status.ACTIVE : Status.INACTIVE);
                        return perm;
                    })
                    .toList();

            M2MApi m2mApi = new M2MApi(accessClient);

            ResourceDTO resource = new ResourceDTO();

            resource.setName(applicationName);
            resource.setType("API");
            resource.setDescription("Resource for application: " + applicationName);

            LOGGER.info("[Authorization Sync] Synchronizing resource for application '{}'", applicationName);

            m2mApi.syncResources(resource, m2mToken, applicationName);

            LOGGER.info("[Authorization Sync] Resource synchronization completed.");

            LOGGER.info("[Authorization Sync] Synchronizing {} permissions for application '{}'", permissions.size(), applicationName);

            m2mApi.syncPermissions(permissions, m2mToken, applicationName);

            LOGGER.info("[Permission Sync] Successfully synchronized {} permissions.", permissions.size());

        } catch (Exception ex) {
            LOGGER.error("[Permission Sync] Failed to synchronize authorization with Access Management API", ex);
        }
    }
}