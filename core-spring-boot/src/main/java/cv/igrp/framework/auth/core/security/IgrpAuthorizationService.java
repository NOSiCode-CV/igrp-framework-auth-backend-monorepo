package cv.igrp.framework.auth.core.security;

import cv.igrp.framework.auth.generated.PermissionsRegistry;
import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.api.AuthorizeApi;
import cv.igrp.platform.access.client.model.PermissionCheckRequestDTO;
import cv.igrp.platform.access.client.model.PermissionCheckResponseDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service("igrpAuthorization")
@SuppressWarnings("unused")
public class IgrpAuthorizationService {

    private final ApiClient client;
    private final AuthenticationHelper authHelper;

    public IgrpAuthorizationService(ApiClient client, AuthenticationHelper authHelper) {
        this.client = client;
        this.authHelper = authHelper;
    }

    /**
     * Checks if the current user has a specific permission.
     *
     * @param action the permission enum (e.g. "Permission.FINANCE_SALARY_VIEW")
     * @return true if allowed, false otherwise
     */
    public boolean checkPermission(PermissionsRegistry.Permission action) {
        try {
            String token = authHelper.getToken();
            client.setAuthToken(token);
            AuthorizeApi authorizeApi = new AuthorizeApi(client);

            return authorizeApi.checkAuthorization(
                    new PermissionCheckRequestDTO(null, action.getCode())
            ).isAllowed();
        } catch (Exception e) {
            throw new RuntimeException("Error checking permission: " + action, e);
        }
    }

    /**
     * Checks if the user has ALL the given permissions.
     *
     * @param actions list or varargs of permission enums
     * @return true only if ALL are allowed
     */
    public boolean checkAllPermissions(PermissionsRegistry.Permission... actions) {
        if (actions == null || actions.length == 0) {
            return false;
        }

        try {
            String token = authHelper.getToken();
            client.setAuthToken(token);
            AuthorizeApi authorizeApi = new AuthorizeApi(client);

            List<PermissionCheckRequestDTO> requests = new ArrayList<>();
            Arrays.stream(actions).forEach(a -> requests.add(new PermissionCheckRequestDTO(null, a.getCode())));

            List<PermissionCheckResponseDTO> responses =
                    authorizeApi.batchCheckAuthorization(requests);

            // Return true only if all are allowed
            return responses.stream().allMatch(PermissionCheckResponseDTO::isAllowed);

        } catch (Exception e) {
            throw new RuntimeException("Error checking all permissions: " + Arrays.toString(actions), e);
        }
    }

    /**
     * Checks if the user has ANY of the given permissions.
     *
     * @param actions list or varargs of permission enums
     * @return true if at least one is allowed
     */
    public boolean checkAnyPermission(PermissionsRegistry.Permission... actions) {
        if (actions == null || actions.length == 0) {
            return false;
        }

        try {
            String token = authHelper.getToken();
            client.setAuthToken(token);
            AuthorizeApi authorizeApi = new AuthorizeApi(client);

            List<PermissionCheckRequestDTO> requests = new ArrayList<>();
            Arrays.stream(actions).forEach(a -> requests.add(new PermissionCheckRequestDTO(null, a.getCode())));

            List<PermissionCheckResponseDTO> responses =
                    authorizeApi.batchCheckAuthorization(requests);

            // Return true if at least one permission is allowed
            return responses.stream().anyMatch(PermissionCheckResponseDTO::isAllowed);

        } catch (Exception e) {
            throw new RuntimeException("Error checking any permissions: " + Arrays.toString(actions), e);
        }
    }
}