package cv.igrp.framework.auth.autentika.adapter;

import cv.igrp.framework.auth.core.adapter.IAdapter;
import cv.igrp.framework.auth.autentika.client.AutentikaClient;
import cv.igrp.framework.auth.autentika.config.AutentikaConfiguration;
import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.core.model.*;

import java.time.Instant;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Autentika integration through the gateway's OAuth DCR v1.1 API.
 * Here an OAuth application is a client registration: application methods look
 * it up by client_name, while client methods address it by client_id.
 * Application names are tenant-wide; departmentCode is not mapped to SCIM.
 * Name-only creation defaults to client_credentials. Use createClient to receive
 * credentials and configure grants/redirect URIs. Secret rotation is unsupported.
 * Other mutations are unsupported; remaining queries are placeholders.
 */
@SuppressWarnings("unused")
public class AutentikaAdapter implements IAdapter {

    private final AutentikaClient client;

    public AutentikaAdapter(AutentikaConfiguration configuration) {
        this.client = new AutentikaClient(configuration);
    }

    public AutentikaAdapter(AutentikaConfiguration configuration, HttpClient http) {
        this.client = new AutentikaClient(configuration, http);
    }

    @Override
    public void createApplication(String departmentCode, String applicationCode) throws IAMException {
        client.createApplication(applicationCode);
    }

    @Override
    public void updateApplication(String departmentCode, String applicationCode, String newCode) throws IAMException {
        client.updateApplication(applicationCode, newCode);
    }

    @Override
    public void deleteApplication(String departmentCode, String applicationCode) throws IAMException {
        client.deleteApplication(applicationCode);
    }

    @Override
    public IssuedClient createClient(ClientRegistration registration) throws IAMException {
        return client.createClient(registration);
    }

    @Override
    public IssuedClient rotateClientSecret(String clientId) throws IAMException {
        return client.rotateClientSecret();
    }

    @Override
    public void updateClient(String clientId, ClientRegistration registration) throws IAMException {
        client.updateClient(clientId, registration);
    }

    @Override
    public void deleteClient(String clientId) throws IAMException {
        client.deleteClient(clientId);
    }

    @Override
    public Optional<Instant> authenticateClient(String clientId, String clientSecret) throws IAMException {
        return client.authenticateClient(clientId, clientSecret);
    }


    // Mutating operations below are not supported yet.

    @Override
    public void createDepartment(String departmentCode, String parentDepartment) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support createDepartment yet.");
    }

    @Override
    public void updateDepartment(String departmentCode, String newCode) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support updateDepartment yet.");
    }

    @Override
    public void deleteDepartment(String departmentCode) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support deleteDepartment yet.");
    }

    @Override
    public void createRole(String departmentCode, String roleName) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support createRole yet.");
    }

    @Override
    public void updateRole(String departmentCode, String roleName, String newName) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support updateRole yet.");
    }

    @Override
    public void deleteRole(String departmentCode, String roleName) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support deleteRole yet.");
    }

    @Override
    public void assignRoleToUser(String departmentCode, String roleName, String sub) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support assignRoleToUser yet.");
    }

    @Override
    public void unassignRoleFromUser(String departmentCode, String roleName, String sub) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support unassignRoleFromUser yet.");
    }

    @Override
    public void createUser(UserIdentity userIdentity) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support createUser yet.");
    }

    @Override
    public Optional<UserIdentity> resolveUser(String email) {
        return Optional.empty();
    }

    @Override
    public void createPermission(String permissionName, String description) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support createPermission yet.");
    }

    @Override
    public void deletePermission(String permissionName) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support deletePermission yet.");
    }

    @Override
    public void assignPermissionToRoles(String permissionName, Set<String> roleNames) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support assignPermissionToRoles yet.");
    }

    @Override
    public void assignPermissionsToRole(Set<String> permissionNames, String roleName) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support assignPermissionsToRole yet.");
    }

    @Override
    public void unassignPermissionFromRoles(String permissionName, Set<String> roleNames) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support unassignPermissionFromRoles yet.");
    }

    @Override
    public void unassignPermissionsFromRole(Set<String> permissionNames, String roleName) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support unassignPermissionsFromRole yet.");
    }

    @Override
    public void createResource(String resourceName, String description, List<String> uris, List<String> scopes) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support createResource yet.");
    }

    @Override
    public void deleteResource(String resourceName) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support deleteResource yet.");
    }

    @Override
    public List<DepartmentInfo> getAllDepartments() throws IAMException {
        return List.of();
    }

    @Override
    public Optional<DepartmentInfo> getDepartment(String departmentCode) throws IAMException {
        return Optional.empty();
    }

    @Override
    public List<ApplicationInfo> getAllApplications() throws IAMException {
        return List.of();
    }

    @Override
    public Optional<ApplicationInfo> getApplication(String departmentCode, String applicationCode) throws IAMException {
        return Optional.empty();
    }

    @Override
    public List<RoleInfo> getAllRoles() throws IAMException {
        return List.of();
    }

    @Override
    public Optional<RoleInfo> getRole(String departmentCode, String roleName) throws IAMException {
        return Optional.empty();
    }

    @Override
    public List<UserIdentity> getAllUsers() throws IAMException {
        return List.of();
    }

    @Override
    public Map<String, Set<String>> getUserRoles(String sub) throws IAMException {
        return Map.of();
    }

    @Override
    public Set<String> getRoleUsers(String departmentCode, String roleName) throws IAMException {
        return Set.of();
    }

    @Override
    public Map<String, Map<String, Set<String>>> getAllUserRoles() throws IAMException {
        return Map.of();
    }

    @Override
    public List<PermissionInfo> getAllPermissions() throws IAMException {
        return List.of();
    }

    @Override
    public Optional<PermissionInfo> getPermission(String permissionName) throws IAMException {
        return Optional.empty();
    }

    @Override
    public Set<String> getPermissionRoles(String permissionName) throws IAMException {
        return Set.of();
    }

    @Override
    public Set<String> getRolePermissions(String departmentCode, String roleName) throws IAMException {
        return Set.of();
    }

    @Override
    public Map<String, Set<String>> getAllRolePermissions() throws IAMException {
        return Map.of();
    }

    @Override
    public List<ResourceInfo> getAllResources() throws IAMException {
        return List.of();
    }

    @Override
    public Optional<ResourceInfo> getResource(String resourceName) throws IAMException {
        return Optional.empty();
    }

    @Override
    public void resetProvider() throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support resetProvider yet.");
    }

    @Override
    public boolean departmentExists(String departmentCode) throws IAMException {
        return false;
    }

    @Override
    public boolean applicationExists(String departmentCode, String applicationCode) throws IAMException {
        return false;
    }

    @Override
    public boolean roleExists(String departmentCode, String roleName) throws IAMException {
        return false;
    }

    @Override
    public boolean permissionExists(String permissionName) throws IAMException {
        return false;
    }

    @Override
    public boolean resourceExists(String resourceName) throws IAMException {
        return false;
    }

    @Override
    public boolean protocolMapperExists(String name) throws IAMException {
        return false;
    }

    @Override
    public void createJwtRolesClaimMapper(String claimName, String claimDescription) throws IAMException {
        throw new UnsupportedOperationException("Autentika does not support createJwtRolesClaimMapper yet.");
    }

    @Override
    public List<String> getAllClientProtocolMappers() throws IAMException {
        return List.of();
    }

}
