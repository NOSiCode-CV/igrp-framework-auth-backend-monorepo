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
 * Automatically synchronizes code-defined permissions with the Access
 * Management API on application startup.
 *
 * <p>Authentication uses OAuth2 {@code client_credentials} against the
 * Access Management's authorization server ({@code POST /oauth2/token}).
 * The token is fetched and cached by the client library's
 * {@code ClientCredentialsTokenProvider} — wire the credentials via
 * the following properties:
 * <pre>{@code
 * igrp.access.m2m.client-id     = my-service
 * igrp.access.m2m.client-secret = ${MY_SERVICE_CLIENT_SECRET}
 * # optional — when the client registration constrains scope
 * igrp.access.m2m.scope         = m2m
 * }</pre>
 *
 * <p>The legacy static-token property
 * {@code igrp.access.m2m.sync-token} is still read for backwards
 * compatibility; when only the legacy token is set the runner falls
 * back to the deprecated {@code M2MApi} signatures. New deployments
 * should use the client-credentials properties.
 */
@Component
public class AuthorizationSyncRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationSyncRunner.class);

    private final ApiClient accessClient;

    @Value("${igrp.access.m2m.client-id:}")
    private String clientId;

    @Value("${igrp.access.m2m.client-secret:}")
    private String clientSecret;

    @Value("${igrp.access.m2m.scope:}")
    private String scope;

    /** @deprecated retained for callers still on the static-token model. */
    @Deprecated
    @Value("${igrp.access.m2m.sync-token:}")
    private String legacyM2mToken;

    @Value("${spring.application.name:}")
    private String applicationName;

    public AuthorizationSyncRunner(ApiClient accessClient) {
        this.accessClient = accessClient;
    }

    @PostConstruct
    public void syncAuthorization() {
        try {
            LOGGER.info("[Authorization Sync] Starting authorization synchronization with Access Management API...");

            // Configure OAuth2 client_credentials once per app boot so every
            // M2MApi call thereafter authenticates transparently. The client
            // library fetches the token from POST /oauth2/token and caches
            // it across calls with a leeway-based refresh.
            boolean useClientCredentials = clientId != null && !clientId.isBlank()
                    && clientSecret != null && !clientSecret.isBlank();

            if (useClientCredentials) {
                if (scope != null && !scope.isBlank()) {
                    accessClient.setClientCredentials(clientId, clientSecret, scope);
                } else {
                    accessClient.setClientCredentials(clientId, clientSecret);
                }
                LOGGER.info("[Authorization Sync] Authenticating to Access Management via client_credentials grant (clientId={})", clientId);
            } else {
                LOGGER.warn("[Authorization Sync] No client_credentials configured ({}={}, {}={}). Falling back to deprecated static-token mode.",
                        "igrp.access.m2m.client-id", maskBlank(clientId),
                        "igrp.access.m2m.client-secret", clientSecret == null || clientSecret.isBlank() ? "(blank)" : "(set)");
            }

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

            if (useClientCredentials) {
                m2mApi.syncResources(resource, applicationName);
            } else {
                //noinspection deprecation — legacy path until callers migrate.
                m2mApi.syncResources(resource, legacyM2mToken, applicationName);
            }

            LOGGER.info("[Authorization Sync] Resource synchronization completed.");

            LOGGER.info("[Authorization Sync] Synchronizing {} permissions for application '{}'", permissions.size(), applicationName);

            if (useClientCredentials) {
                m2mApi.syncPermissions(permissions, applicationName);
            } else {
                //noinspection deprecation — legacy path until callers migrate.
                m2mApi.syncPermissions(permissions, legacyM2mToken, applicationName);
            }

            LOGGER.info("[Permission Sync] Successfully synchronized {} permissions.", permissions.size());

        } catch (Exception ex) {
            LOGGER.error("[Permission Sync] Failed to synchronize authorization with Access Management API", ex);
        }
    }

    private static String maskBlank(String value) {
        return value == null || value.isBlank() ? "(blank)" : value;
    }
}
