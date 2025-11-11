package cv.igrp.framework.auth.keycloak.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cv.igrp.framework.auth.core.adapter.IAdapter;
import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.core.model.*;
import cv.igrp.framework.auth.keycloak.client.KeycloakClientFactory;
import cv.igrp.framework.auth.keycloak.config.KeycloakProperties;
import cv.igrp.framework.auth.keycloak.constants.PolicyType;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.idm.authorization.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@SuppressWarnings("unused")
public class KeycloakAdapter implements IAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakAdapter.class);
    public static final String POLICY_SUFFIX = "_p";
    public static final String GROUP_PATH_SEPARATOR = "/";
    public static final String CLIENT_PREFIX = "client_";
    public static final String APP_NAME_PREFIX = "app_";
    private static final String ATTR_TYPE = "igrp_type";
    private static final String TYPE_APPLICATION = "application";
    private static final String TYPE_DEPARTMENT = "department";
    private static final String TYPE_ROLE = "role";

    private final KeycloakClientFactory keycloakClientFactory;
    private final KeycloakProperties keycloakProperties;

    public KeycloakAdapter(KeycloakClientFactory keycloakClientFactory, KeycloakProperties keycloakProperties) {
        this.keycloakClientFactory = keycloakClientFactory;
        this.keycloakProperties = keycloakProperties;
    }

    @Override
    public void createDepartment(String departmentCode, String parentDepartment) throws IAMException {
        LOGGER.info("[createDepartment] Input params: departmentCode={}, parentDepartment={}", departmentCode, parentDepartment);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            GroupsResource groupsResource = realmResource.groups();

            GroupRepresentation newDepartment = new GroupRepresentation();
            newDepartment.setName(departmentCode);

            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put(ATTR_TYPE, List.of(TYPE_DEPARTMENT));
            newDepartment.setAttributes(attributes);

            if (parentDepartment != null && !parentDepartment.isBlank()) {
                LOGGER.info("[createDepartment] Fetching parent group by path: {}", parentDepartment);
                GroupRepresentation parentGroupRep = findDepartmentByName(realmResource, parentDepartment);
                LOGGER.info("[createDepartment] Parent group fetched: {}", parentGroupRep != null ? parentGroupRep.getName() : null);
                if (parentGroupRep == null) {
                    throw new IAMException("Parent department not found: " + parentDepartment);
                }
                GroupResource parentGroup = groupsResource.group(parentGroupRep.getId());

                try (Response response = parentGroup.subGroup(newDepartment)) {
                    LOGGER.info("[createDepartment] Subgroup creation response status: {}", response.getStatus());
                    if (response.getStatus() != 201) {
                        throw new IAMException("Error creating department. HTTP Status: " + response.getStatus());
                    }
                }
            } else {
                try (Response response = groupsResource.add(newDepartment)) {
                    LOGGER.info("[createDepartment] Group creation response status: {}", response.getStatus());
                    if (response.getStatus() != 201) {
                        throw new IAMException("Error creating department. HTTP Status: " + response.getStatus());
                    }
                }
            }
            LOGGER.info("Department created: {}", departmentCode);
        } catch (Exception ex) {
            LOGGER.error("Failed to create department '{}'", departmentCode, ex);
            throw new IAMException("Error creating department", ex);
        }
    }

    @Override
    public void updateDepartment(String departmentCode, String newCode) throws IAMException {
        LOGGER.info("[updateDepartment] Input params: departmentCode={}, newCode={}", departmentCode, newCode);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            GroupRepresentation department = findDepartmentByName(realmResource, departmentCode);
            LOGGER.info("[updateDepartment] Fetched department: {}", department != null ? department.getName() : null);
            if (department == null) {
                throw new IAMException("Department not found: " + departmentCode);
            }
            GroupResource departmentResource = realmResource.groups().group(department.getId());

            GroupRepresentation updatedDepartment = departmentResource.toRepresentation();
            updatedDepartment.setName(newCode);
            departmentResource.update(updatedDepartment);

            LOGGER.info("Department '{}' updated to '{}'", departmentCode, newCode);
        } catch (Exception ex) {
            LOGGER.error("Failed to update department '{}'", departmentCode, ex);
            throw new IAMException("Error updating department", ex);
        }
    }

    @Override
    public void deleteDepartment(String departmentCode) throws IAMException {
        LOGGER.info("[deleteDepartment] Input param: departmentCode={}", departmentCode);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            GroupRepresentation department = findDepartmentByName(realmResource, departmentCode);
            LOGGER.info("[deleteDepartment] Fetched department: {}", department != null ? department.getName() : null);
            if (department == null) {
                throw new IAMException("Department not found: " + departmentCode);
            }
            realmResource.groups().group(department.getId()).remove();

            LOGGER.info("Department '{}' deleted", departmentCode);
        } catch (Exception ex) {
            LOGGER.error("Failed to delete department '{}'", departmentCode, ex);
            throw new IAMException("Error deleting department", ex);
        }
    }

    @Override
    public void createApplication(String departmentCode, String applicationCode) throws IAMException {
        LOGGER.info("[createApplication] Input params: departmentCode={}, applicationCode={}", departmentCode, applicationCode);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());

            LOGGER.info("[createApplication] Fetching department group by name: {}", departmentCode);
            GroupRepresentation departmentGroup = findDepartmentByName(realmResource, departmentCode);
            LOGGER.info("[createApplication] Department group fetched: {}", departmentGroup != null ? departmentGroup.getName() : null);
            if (departmentGroup == null) {
                throw new IAMException("Department not found: " + departmentCode);
            }
            GroupResource departmentResource = realmResource.groups().group(departmentGroup.getId());

            GroupRepresentation applicationGroup = new GroupRepresentation();
            applicationGroup.setName(normalizeAppName(applicationCode));

            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put(ATTR_TYPE, List.of(TYPE_APPLICATION));
            applicationGroup.setAttributes(attributes);

            try (Response response = departmentResource.subGroup(applicationGroup)) {
                LOGGER.info("[createApplication] Subgroup creation response status: {}", response.getStatus());
                if (response.getStatus() != 201) {
                    throw new IAMException("Error creating application. HTTP Status: " + response.getStatus());
                }
            }

            var clientId = CLIENT_PREFIX + applicationCode;
            ClientRepresentation client = new ClientRepresentation();
            client.setName(applicationCode);
            client.setClientId(clientId);
            client.setProtocol("openid-connect");
            client.setRedirectUris(List.of("*"));
            client.setWebOrigins(List.of("*"));
            client.setDescription("Autogenerated for application <%s>".formatted(applicationCode));
            client.setServiceAccountsEnabled(true);
            client.setAuthorizationServicesEnabled(true);
            client.setDirectAccessGrantsEnabled(true);
            client.setStandardFlowEnabled(true);
            client.setPublicClient(false);
            client.setFullScopeAllowed(true);

            try (Response clientResponse = realmResource.clients().create(client)) {
                LOGGER.info("[createApplication] Client creation response status: {}", clientResponse.getStatus());
                if (clientResponse.getStatus() < 200 || clientResponse.getStatus() >= 300) {
                    throw new IAMException("Error creating application client. HTTP Status: " + clientResponse.getStatus());
                }
            }

            LOGGER.info("Application '{}' created in department '{}'", applicationCode, departmentCode);
        } catch (Exception ex) {
            LOGGER.error("Failed to create application '{}'", applicationCode, ex);
            throw new IAMException("Error creating application", ex);
        }
    }

    @Override
    public void updateApplication(String departmentCode, String applicationCode, String newCode) throws IAMException {
        LOGGER.info("[updateApplication] Input params: departmentCode={}, applicationCode={}, newCode={}", departmentCode, applicationCode, newCode);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());

            // Use proper application lookup
            Optional<ApplicationInfo> applicationOpt = getApplication(departmentCode, applicationCode);
            if (applicationOpt.isEmpty()) {
                throw new IAMException("Application not found: " + applicationCode + " in department " + departmentCode);
            }

            // Find the application group using the proper hierarchical lookup
            String appPath = findApplicationPath(realmResource, departmentCode, applicationCode);
            if (appPath == null) {
                throw new IAMException("Application not found: " + applicationCode + " in department " + departmentCode);
            }

            GroupRepresentation application = findGroupByPath(realmResource, appPath);
            LOGGER.info("[updateApplication] Application group fetched: {}", application != null ? application.getName() : null);
            if (application == null) {
                throw new IAMException("Application not found: " + appPath);
            }
            GroupResource applicationResource = realmResource.groups().group(application.getId());

            GroupRepresentation updatedApplication = applicationResource.toRepresentation();
            updatedApplication.setName(normalizeAppName(newCode));
            applicationResource.update(updatedApplication);

            LOGGER.info("Application '{}' updated to '{}'", applicationCode, newCode);
        } catch (Exception ex) {
            LOGGER.error("Failed to update application '{}'", applicationCode, ex);
            throw new IAMException("Error updating application", ex);
        }
    }

    @Override
    public void deleteApplication(String departmentCode, String applicationCode) throws IAMException {
        LOGGER.info("[deleteApplication] Input params: departmentCode={}, applicationCode={}", departmentCode, applicationCode);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());

            // Use proper application lookup
            String appPath = findApplicationPath(realmResource, departmentCode, applicationCode);
            if (appPath == null) {
                throw new IAMException("Application not found: " + applicationCode + " in department " + departmentCode);
            }

            GroupRepresentation application = findGroupByPath(realmResource, appPath);
            LOGGER.info("[deleteApplication] Application group fetched: {}", application != null ? application.getName() : null);
            if (application == null) {
                throw new IAMException("Application not found: " + appPath);
            }
            realmResource.groups().group(application.getId()).remove();

            LOGGER.info("Application '{}' deleted from department '{}'", applicationCode, departmentCode);
        } catch (Exception ex) {
            LOGGER.error("Failed to delete application '{}'", applicationCode, ex);
            throw new IAMException("Error deleting application", ex);
        }
    }

    @Override
    public void createRole(String departmentCode, String roleName) throws IAMException {
        LOGGER.info("[createRole] Input params: departmentCode={}, roleName={}", departmentCode, roleName);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            final String clientUUID = getClientInternalId(realmResource);

            LOGGER.info("[createRole] Fetching parent department by path: {}", departmentCode);
            GroupRepresentation parentGroup = findDepartmentByName(realmResource, departmentCode);
            LOGGER.info("[createRole] Parent department fetched: {}", parentGroup != null ? parentGroup.getName() : null);
            if (parentGroup == null) {
                throw new IAMException("Department not found: " + departmentCode);
            }

            GroupRepresentation roleParentGroup = findRoleParentGroup(realmResource, parentGroup);
            LOGGER.info("[createRole] Role parent group: {}", roleParentGroup != null ? roleParentGroup.getName() : null);
            GroupResource parentGroupResource = realmResource.groups().group(Objects.requireNonNull(roleParentGroup).getId());

            GroupRepresentation roleGroup = new GroupRepresentation();
            roleGroup.setName(roleName);

            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put(ATTR_TYPE, List.of(TYPE_ROLE));
            roleGroup.setAttributes(attributes);

            try (Response response = parentGroupResource.subGroup(roleGroup)) {
                LOGGER.info("[createRole] Subgroup creation response status: {}", response.getStatus());
                if (response.getStatus() != 201) {
                    throw new IAMException("Error creating role group. HTTP Status: " + response.getStatus());
                }
            }

            RoleRepresentation role = new RoleRepresentation();
            role.setName(roleName);
            role.setDescription("Role in department: " + departmentCode);
            realmResource.clients().get(clientUUID).roles().create(role);

            RoleRepresentation persistedRole = realmResource.clients().get(clientUUID).roles().get(roleName).toRepresentation();

            // Use the actual created group path
            String createdRolePath = buildGroupPath(roleParentGroup) + GROUP_PATH_SEPARATOR + roleName;
            GroupRepresentation createdRoleGroup = findGroupByPath(realmResource, createdRolePath);
            LOGGER.info("[createRole] Created role group: {}", createdRoleGroup != null ? createdRoleGroup.getName() : null);

            if (createdRoleGroup != null) {
                realmResource.groups()
                        .group(createdRoleGroup.getId())
                        .roles()
                        .clientLevel(clientUUID)
                        .add(List.of(persistedRole));
            }

            LOGGER.info("Role '{}' created in department '{}'", roleName, departmentCode);
        } catch (Exception ex) {
            LOGGER.error("Failed to create role '{}'", roleName, ex);
            throw new IAMException("Error creating role", ex);
        }
    }

    @Override
    public void updateRole(String departmentCode, String roleName, String newName) throws IAMException {
        LOGGER.info("[updateRole] Input params: departmentCode={}, roleName={}, newName={}", departmentCode, roleName, newName);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            final String clientUUID = getClientInternalId(realmResource);

            // Use proper role lookup that handles hierarchical paths
            String roleGroupPath = findRoleGroupPath(realmResource, departmentCode, roleName);
            LOGGER.info("[updateRole] Role group path: {}", roleGroupPath);
            if (roleGroupPath == null) {
                throw new IAMException("Role not found: " + roleName + " in department " + departmentCode);
            }

            GroupRepresentation roleGroup = findGroupByPath(realmResource, roleGroupPath);
            LOGGER.info("[updateRole] Role group fetched: {}", roleGroup != null ? roleGroup.getName() : null);
            if (roleGroup == null) {
                throw new IAMException("Role group not found for path: " + roleGroupPath);
            }

            GroupResource roleGroupResource = realmResource.groups().group(roleGroup.getId());

            GroupRepresentation updatedRoleGroup = roleGroupResource.toRepresentation();
            updatedRoleGroup.setName(newName);
            roleGroupResource.update(updatedRoleGroup);

            RoleResource roleResource = realmResource.clients().get(clientUUID).roles().get(roleName);
            RoleRepresentation role = roleResource.toRepresentation();
            role.setName(newName);
            roleResource.update(role);

            LOGGER.info("Role '{}' updated to '{}'", roleName, newName);
        } catch (Exception ex) {
            LOGGER.error("Failed to update role '{}'", roleName, ex);
            throw new IAMException("Error updating role", ex);
        }
    }

    @Override
    public void deleteRole(String departmentCode, String roleName) throws IAMException {
        LOGGER.info("[deleteRole] Input params: departmentCode={}, roleName={}", departmentCode, roleName);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            final String clientUUID = getClientInternalId(realmResource);

            realmResource.clients().get(clientUUID).roles().deleteRole(roleName);

            // Use proper role lookup that handles hierarchical paths
            String roleGroupPath = findRoleGroupPath(realmResource, departmentCode, roleName);
            LOGGER.info("[deleteRole] Role group path: {}", roleGroupPath);
            if (roleGroupPath == null) {
                throw new IAMException("Role not found: " + roleName + " in department " + departmentCode);
            }

            GroupRepresentation roleGroup = findGroupByPath(realmResource, roleGroupPath);
            LOGGER.info("[deleteRole] Role group fetched: {}", roleGroup != null ? roleGroup.getName() : null);
            if (roleGroup == null) {
                throw new IAMException("Role group not found for path: " + roleGroupPath);
            }

            realmResource.groups().group(roleGroup.getId()).remove();

            LOGGER.info("Role '{}' deleted from department '{}'", roleName, departmentCode);

        } catch (Exception ex) {
            LOGGER.error("Failed to delete role '{}'", roleName, ex);
            throw new IAMException("Error deleting role", ex);
        }
    }

    @Override
    public void createUser(UserIdentity userIdentity) throws IAMException {
        LOGGER.info("[createUser] Input param: userIdentity={}", userIdentity);
        try(Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());

            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setUsername(userIdentity.getUsername());
            userRepresentation.setEmail(userIdentity.getEmail());
            userRepresentation.setFirstName(userIdentity.getFirstName());
            userRepresentation.setLastName(userIdentity.getLastName());
            userRepresentation.setEmailVerified(userIdentity.isEmailVerified());
            userRepresentation.setEnabled(userIdentity.isEnabled());

            try (Response userResponse = realmResource.users().create(userRepresentation)) {
                LOGGER.info("[createUser] User creation response status: {}", userResponse.getStatus());
                if (!(userResponse.getStatus() >= 200 && userResponse.getStatus() < 300))
                    throw new IAMException("Error creating user. HTTP Status: " + userResponse.getStatus());
                LOGGER.info("User created: {}", ofNullable(userResponse.getEntity()).orElse(userIdentity.getUsername()));
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to create user '{}'", userIdentity.getUsername(), ex);
            throw new IAMException("Error creating user", ex);
        }
    }

    @Override
    public Optional<UserIdentity> resolveUser(String username) {
        LOGGER.info("[resolveUser] Input param: username={}", username);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            List<UserRepresentation> foundUsers = realmResource.users().search(username, true);
            LOGGER.info("[resolveUser] Users found: {}", foundUsers.size());
            return foundUsers
                    .stream()
                    .findFirst()
                    .map(user -> {
                        LOGGER.info("User '{}' resolved successfully", username);
                        return IGRPUserRepresentation.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .enable(true)
                                .build();
                    });
        } catch (Exception ex) {
            LOGGER.error("Failed when resolving user '{}'", username, ex);
            return Optional.empty();
        }
    }

    @Override
    public void assignRoleToUser(String departmentCode, String roleName, String username) throws IAMException {
        LOGGER.info("[assignRoleToUser] Input params: departmentCode={}, roleName={}, username={}", departmentCode, roleName, username);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());

            // Use proper role lookup that handles hierarchical paths
            String roleGroupPath = findRoleGroupPath(realmResource, departmentCode, roleName);
            LOGGER.info("[assignRoleToUser] Role group path: {}", roleGroupPath);
            if (roleGroupPath == null) {
                throw new IAMException("Role not found: " + roleName + " in department " + departmentCode);
            }

            GroupRepresentation roleGroup = findGroupByPath(realmResource, roleGroupPath);
            LOGGER.info("[assignRoleToUser] Role group fetched: {}", roleGroup != null ? roleGroup.getName() : null);
            if (roleGroup == null) {
                throw new IAMException("Role group not found for path: " + roleGroupPath);
            }

            UserIdentity user = resolveUser(username)
                    .orElseThrow(() -> new IAMException("User not found: " + username));
            LOGGER.info("[assignRoleToUser] User fetched: {}", user.getUsername());
            realmResource.users().get(user.getId()).joinGroup(roleGroup.getId());
            LOGGER.info("User '{}' assigned to role '{}' in department '{}'", username, roleName, departmentCode);
        } catch (Exception ex) {
            LOGGER.error("Failed to assign role '{}' to user '{}'", roleName, username, ex);
            throw new IAMException("Error assigning role to user", ex);
        }
    }

    @Override
    public void unassignRoleFromUser(String departmentCode, String roleName, String username) throws IAMException {
        LOGGER.info("[unassignRoleFromUser] Input params: departmentCode={}, roleName={}, username={}", departmentCode, roleName, username);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());

            // Use proper role lookup that handles hierarchical paths
            String roleGroupPath = findRoleGroupPath(realmResource, departmentCode, roleName);
            LOGGER.info("[unassignRoleFromUser] Role group path: {}", roleGroupPath);
            if (roleGroupPath == null) {
                throw new IAMException("Role not found: " + roleName + " in department " + departmentCode);
            }

            GroupRepresentation roleGroup = findGroupByPath(realmResource, roleGroupPath);
            LOGGER.info("[unassignRoleFromUser] Role group fetched: {}", roleGroup != null ? roleGroup.getName() : null);
            if (roleGroup == null) {
                throw new IAMException("Role group not found for path: " + roleGroupPath);
            }

            UserIdentity user = resolveUser(username)
                    .orElseThrow(() -> new IAMException("User not found: " + username));
            LOGGER.info("[unassignRoleFromUser] User fetched: {}", user.getUsername());
            realmResource.users().get(user.getId()).leaveGroup(roleGroup.getId());
            LOGGER.info("User '{}' unassigned from role '{}' in department '{}'", username, roleName, departmentCode);
        } catch (Exception ex) {
            LOGGER.error("Failed to unassign role '{}' from user '{}'", roleName, username, ex);
            throw new IAMException("Error unassigning role from user", ex);
        }
    }

    @Override
    public void createPermission(String permissionName, String description) throws IAMException {
        LOGGER.info("[createPermission] Input params: permissionName={}, description={}", permissionName, description);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);
            String policyName = permissionName + POLICY_SUFFIX;
            AuthorizationResource authorizationResource = realmResource.clients().get(clientId).authorization();

            PolicyRepresentation policy = new PolicyRepresentation();
            policy.setName(policyName);
            policy.setDescription(description);
            policy.setLogic(Logic.POSITIVE);
            policy.setType(PolicyType.ROLE.getName());
            try (Response response = authorizationResource.policies().create(policy)) {
                LOGGER.info("[createPermission] Policy creation response status: {}", response.getStatus());
                if (response.getStatus() != 201) {
                    throw new IAMException("Failed to create policy. HTTP Status: " + response.getStatus());
                }
            }

            ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
            permission.setName(permissionName);
            permission.setDescription(description);
            permission.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
            permission.setLogic(Logic.POSITIVE);
            permission.setPolicies(Set.of(policyName));

            try (Response response = authorizationResource
                    .permissions()
                    .resource()
                    .create(permission)) {
                LOGGER.info("[createPermission] Permission creation response status: {}", response.getStatus());
                if (response.getStatus() != 201) {
                    throw new IAMException("Failed to create permission. HTTP Status: " + response.getStatus());
                }
            }

            LOGGER.info("Permission '{}' created", permissionName);
        } catch (Exception ex) {
            LOGGER.error("Failed to create permission '{}'", permissionName, ex);
            throw new IAMException("Error creating permission", ex);
        }
    }

    @Override
    public void deletePermission(String permissionName) throws IAMException {
        LOGGER.info("[deletePermission] Input param: permissionName={}", permissionName);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);
            String policyName = permissionName + POLICY_SUFFIX;
            AuthorizationResource authorization = realmResource.clients().get(clientId).authorization();

            ResourcePermissionRepresentation resourcePermission = authorization.permissions().resource().findByName(permissionName);
            authorization.permissions().resource().findById(resourcePermission.getId()).remove();

            PolicyRepresentation policy = authorization.policies().findByName(policyName);
            authorization.policies().policy(policy.getId()).remove();

            LOGGER.info("[deletePermission] ResourcePermission and Policy deleted for permission: {}", permissionName);
            LOGGER.info("Permission '{}' deleted", permissionName);
        } catch (Exception ex) {
            LOGGER.error("Failed to delete permission '{}'", permissionName, ex);
            throw new IAMException("Error deleting permission", ex);
        }
    }

    @Override
    public void assignPermissionToRoles(String permissionName, Set<String> roleNames) throws IAMException {
        LOGGER.info("[assignPermissionToRoles] Input params: permissionName={}, roleNames={}", permissionName, roleNames);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realm);
            String policyName = permissionName + POLICY_SUFFIX;
            AuthorizationResource authorizationResource = realm.clients().get(clientId).authorization();

            PolicyRepresentation policy = authorizationResource.policies().findByName(policyName);
            List<Map<String, Object>> rolesConfig = roleNames.stream()
                    .map(roleName -> {
                        RoleRepresentation role = realm.clients()
                                .get(clientId)
                                .roles()
                                .get(roleName)
                                .toRepresentation();
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("id", role.getId());
                        entry.put("required", true);
                        return entry;
                    })
                    .collect(Collectors.toList());

            Map<String, String> config = ofNullable(policy.getConfig()).orElse(new HashMap<>());
            config.put("fetchRoles", "false");
            config.put("roles", new ObjectMapper().writeValueAsString(rolesConfig));
            policy.setConfig(config);
            authorizationResource.policies().policy(policy.getId()).update(policy);

            LOGGER.info("[assignPermissionToRoles] Policy updated for permission: {} with roles: {}", permissionName, roleNames);
            LOGGER.info("Permission '{}' assigned to roles {}", permissionName, roleNames);
        } catch (Exception ex) {
            LOGGER.error("Failed to assign permission to roles", ex);
            throw new IAMException("Error assigning permission to roles", ex);
        }
    }

    @Override
    public void unassignPermissionFromRoles(String permissionName, Set<String> roleNames) throws IAMException {
        LOGGER.info("[unassignPermissionFromRoles] Input params: permissionName={}, roleNames={}", permissionName, roleNames);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realm);
            String policyName = permissionName + POLICY_SUFFIX;
            AuthorizationResource authorizationResource = realm.clients().get(clientId).authorization();

            PolicyRepresentation policy = authorizationResource.policies().findByName(policyName);
            String rolesJson = policy.getConfig().get("roles");
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> existingRoles = mapper.readValue(rolesJson, new TypeReference<>() {
            });

            List<Map<String, Object>> updatedRoles = existingRoles.stream()
                    .filter(role -> {
                        String roleId = (String) role.get("id");
                        return roleNames.stream().noneMatch(rn -> {
                            try {
                                RoleRepresentation roleRep = realm.clients().get(clientId).roles().get(rn).toRepresentation();
                                return roleRep.getId().equals(roleId);
                            } catch (Exception e) {
                                return false;
                            }
                        });
                    })
                    .collect(Collectors.toList());

            Map<String, String> config = policy.getConfig();
            config.put("roles", mapper.writeValueAsString(updatedRoles));
            policy.setConfig(config);
            authorizationResource.policies().policy(policy.getId()).update(policy);

            LOGGER.info("[unassignPermissionFromRoles] Policy updated for permission: {} after removing roles: {}", permissionName, roleNames);
            LOGGER.info("Permission '{}' unassigned from roles {}", permissionName, roleNames);
        } catch (Exception ex) {
            LOGGER.error("Failed to unassign permission from roles", ex);
            throw new IAMException("Error unassigning permission from roles", ex);
        }
    }

    @Override
    public void assignPermissionsToRole(Set<String> permissionNames, String roleName) throws IAMException {
        LOGGER.info("[assignPermissionsToRole] Input params: permissionNames={}, roleName={}", permissionNames, roleName);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realm);
            AuthorizationResource authorizationResource = realm.clients().get(clientId).authorization();
            RoleRepresentation role = realm.clients().get(clientId).roles().get(roleName).toRepresentation();

            Map<String, Object> roleEntry = new HashMap<>();
            roleEntry.put("id", role.getId());
            roleEntry.put("required", true);

            for (String permissionName : permissionNames) {
                String policyName = permissionName + POLICY_SUFFIX;
                PolicyRepresentation policy = authorizationResource.policies().findByName(policyName);

                String rolesJson = policy.getConfig().get("roles");
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> existingRoles = mapper.readValue(rolesJson, new TypeReference<>() {
                });

                boolean alreadyExists = existingRoles.stream()
                        .anyMatch(entry -> role.getId().equals(entry.get("id")));

                if (!alreadyExists) {
                    existingRoles.add(roleEntry);
                    Map<String, String> config = policy.getConfig();
                    config.put("roles", mapper.writeValueAsString(existingRoles));
                    policy.setConfig(config);
                    authorizationResource.policies().policy(policy.getId()).update(policy);
                }
            }

            LOGGER.info("[assignPermissionsToRole] Permissions assigned: {} to role: {}", permissionNames, roleName);
            LOGGER.info("Permissions {} assigned to role '{}'", permissionNames, roleName);
        } catch (Exception ex) {
            LOGGER.error("Failed to assign permissions to role", ex);
            throw new IAMException("Error assigning permissions to role", ex);
        }
    }

    @Override
    public void unassignPermissionsFromRole(Set<String> permissionNames, String roleName) throws IAMException {
        LOGGER.info("[unassignPermissionsFromRole] Input params: permissionNames={}, roleName={}", permissionNames, roleName);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realm);
            AuthorizationResource authorizationResource = realm.clients().get(clientId).authorization();
            RoleRepresentation role = realm.clients().get(clientId).roles().get(roleName).toRepresentation();

            for (String permissionName : permissionNames) {
                String policyName = permissionName + POLICY_SUFFIX;
                PolicyRepresentation policy = authorizationResource.policies().findByName(policyName);

                String rolesJson = policy.getConfig().get("roles");
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> existingRoles = mapper.readValue(rolesJson, new TypeReference<>() {
                });

                List<Map<String, Object>> updatedRoles = existingRoles.stream()
                        .filter(entry -> !role.getId().equals(entry.get("id")))
                        .collect(Collectors.toList());

                if (updatedRoles.size() != existingRoles.size()) {
                    Map<String, String> config = policy.getConfig();
                    config.put("roles", mapper.writeValueAsString(updatedRoles));
                    policy.setConfig(config);
                    authorizationResource.policies().policy(policy.getId()).update(policy);
                }
            }

            LOGGER.info("[unassignPermissionsFromRole] Permissions unassigned: {} from role: {}", permissionNames, roleName);
            LOGGER.info("Permissions {} unassigned from role '{}'", permissionNames, roleName);
        } catch (Exception ex) {
            LOGGER.error("Failed to unassign permissions from role", ex);
            throw new IAMException("Error unassigning permissions from role", ex);
        }
    }

    @Override
    public void createResource(String resourceName, String description, List<String> uris, List<String> scopes) throws IAMException {
        LOGGER.info("[createResource] Input params: resourceName={}, description={}, uris={}, scopes={}", resourceName, description, uris, scopes);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            final String urn = "urn:" + keycloakProperties.getClientId() + ":" + resourceName;
            RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
            final String clientUUId = getClientInternalId(realm);
            ClientResource clientResource = realm.clients().get(clientUUId);

            ResourceRepresentation resource = new ResourceRepresentation();
            resource.setName(resourceName);
            resource.setDisplayName(description);
            resource.setUris(new HashSet<>(uris));
            resource.setType(urn);
            resource.setOwnerManagedAccess(false);

            if(scopes != null && !scopes.isEmpty()) {
                resource.setScopes(scopes.stream().map(ScopeRepresentation::new).collect(Collectors.toSet()));
            }

            try (Response response = clientResource.authorization().resources().create(resource)) {
                LOGGER.info("[createResource] Resource creation response status: {}", response.getStatus());
                if (response.getStatus() != 201) {
                    throw new IAMException("Error creating resource. HTTP Status: " + response.getStatus());
                }
            }
            LOGGER.info("Resource '{}' created successfully", resourceName);
        } catch (Exception ex) {
            LOGGER.error("Failed to create resource '{}'", resourceName, ex);
            throw new IAMException("Error creating resource", ex);
        }
    }

    @Override
    public void deleteResource(String resourceName) throws IAMException {
        LOGGER.info("[deleteResource] Input param: resourceName={}", resourceName);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realm = keycloak.realm(keycloakProperties.getRealm());
            String clientUUId = getClientInternalId(realm);
            ClientResource clientResource = realm.clients().get(clientUUId);
            AuthorizationResource authorizationResource = clientResource.authorization();

            List<ResourceRepresentation> resources = authorizationResource.resources().findByName(resourceName);
            if (resources == null || resources.isEmpty()) {
                throw new IAMException("Resource not found: " + resourceName);
            }

            authorizationResource.resources().resource(resources.getFirst().getId()).remove();
            LOGGER.info("[deleteResource] Resource deleted: {}", resourceName);
            LOGGER.info("Resource '{}' deleted", resourceName);
        } catch (Exception ex) {
            LOGGER.error("Failed to delete resource '{}'", resourceName, ex);
            throw new IAMException("Error deleting resource", ex);
        }
    }

    @Override
    public List<DepartmentInfo> getAllDepartments() throws IAMException {
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            List<GroupRepresentation> allGroups = getAllGroupsRecursively(realmResource.groups()).stream().filter(
                    group -> !group.getName().startsWith(APP_NAME_PREFIX) && !isRoleGroup(group)
            ).collect(Collectors.toList());

            return allGroups.stream()
                    .map(group -> {
                        DepartmentInfo info = new DepartmentInfo();
                        info.setCode(group.getName());
                        info.setName(group.getName());
                        info.setDescription(group.getAttributes() != null ?
                                group.getAttributes().getOrDefault("description", List.of("")).getFirst() : "");
                        info.setParentDepartment(extractParentDepartment(group.getPath()));
                        info.setStatus("ACTIVE");
                        return info;
                    })
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve all departments", ex);
            throw new IAMException("Error retrieving departments", ex);
        }
    }

    @Override
    public Optional<DepartmentInfo> getDepartment(String departmentCode) throws IAMException {
        LOGGER.info("[getDepartment] Input param: departmentCode={}", departmentCode);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            GroupRepresentation group = findDepartmentByName(realmResource, departmentCode);
            LOGGER.info("[getDepartment] Group fetched: {}", group != null ? group.getName() : null);
            if (group != null) {
                DepartmentInfo info = new DepartmentInfo();
                info.setCode(group.getName());
                info.setName(group.getName());
                info.setDescription(group.getAttributes() != null ?
                        group.getAttributes().getOrDefault("description", List.of("")).getFirst() : "");
                info.setParentDepartment(extractParentDepartment(group.getPath()));
                info.setStatus("ACTIVE");
                LOGGER.info("[getDepartment] DepartmentInfo built: {}", info);
                return Optional.of(info);
            }
            LOGGER.info("[getDepartment] Department not found for code: {}", departmentCode);
            return Optional.empty();
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve department '{}'", departmentCode, ex);
            throw new IAMException("Error retrieving department", ex);
        }
    }

    @Override
    public List<ApplicationInfo> getAllApplications() throws IAMException {
        LOGGER.info("[getAllApplications] Called");
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            List<GroupRepresentation> allGroups = getAllGroupsRecursively(realmResource.groups());
            LOGGER.info("[getAllApplications] Total groups fetched: {}", allGroups.size());
            List<ApplicationInfo> result = allGroups.stream()
                    .filter(group -> isApplicationGroup(group, allGroups))
                    .map(appGroup -> {
                        ApplicationInfo info = new ApplicationInfo();
                        info.setCode(removeAppPrefix(appGroup.getName()));
                        info.setName(removeAppPrefix(appGroup.getName()));
                        info.setDescription(appGroup.getAttributes() != null ?
                                appGroup.getAttributes().getOrDefault("description", List.of("")).getFirst() : "");
                        info.setDepartmentCode(extractParentDepartment(appGroup.getPath()));
                        info.setStatus("ACTIVE");
                        info.setType("APPLICATION");
                        LOGGER.info("[getAllApplications] ApplicationInfo built: {}", info);
                        return info;
                    })
                    .collect(Collectors.toList());
            LOGGER.info("[getAllApplications] Returning {} applications", result.size());
            return result;
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve all applications", ex);
            throw new IAMException("Error retrieving applications", ex);
        }
    }

    @Override
    public Optional<ApplicationInfo> getApplication(String departmentCode, String applicationCode) throws IAMException {
        LOGGER.info("[getApplication] Input params: departmentCode={}, applicationCode={}", departmentCode, applicationCode);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());

            // Use proper application path resolution
            String appPath = findApplicationPath(realmResource, departmentCode, applicationCode);
            if (appPath == null) {
                LOGGER.info("[getApplication] Application not found for departmentCode={}, applicationCode={}", departmentCode, applicationCode);
                return Optional.empty();
            }

            GroupRepresentation appGroup = findGroupByPath(realmResource, appPath);
            LOGGER.info("[getApplication] Group fetched: {}", appGroup != null ? appGroup.getName() : null);
            if (appGroup != null) {
                ApplicationInfo info = new ApplicationInfo();
                info.setCode(removeAppPrefix(appGroup.getName()));
                info.setName(removeAppPrefix(appGroup.getName()));
                info.setDescription(appGroup.getAttributes() != null ?
                        appGroup.getAttributes().getOrDefault("description", List.of("")).getFirst() : "");
                info.setDepartmentCode(departmentCode);
                info.setStatus("ACTIVE");
                info.setType("APPLICATION");
                LOGGER.info("[getApplication] ApplicationInfo built: {}", info);
                return Optional.of(info);
            }
            LOGGER.info("[getApplication] Application not found for departmentCode={}, applicationCode={}", departmentCode, applicationCode);
            return Optional.empty();
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve application '{}' in department '{}'", applicationCode, departmentCode, ex);
            throw new IAMException("Error retrieving application", ex);
        }
    }

    @Override
    public List<RoleInfo> getAllRoles() throws IAMException {
        LOGGER.info("[getAllRoles] Called");
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            final String clientUUID = getClientInternalId(realmResource);
            List<RoleRepresentation> roles = realmResource.clients().get(clientUUID).roles().list();
            LOGGER.info("[getAllRoles] Total roles fetched: {}", roles.size());
            List<RoleInfo> result = roles.stream()
                    .map(role -> {
                        RoleInfo info = new RoleInfo();
                        info.setName(role.getName());
                        info.setDescription(role.getDescription());
                        info.setDepartmentCode(extractDepartmentFromRoleName(role.getName()));
                        info.setStatus("ACTIVE");
                        LOGGER.info("[getAllRoles] RoleInfo built: {}", info);
                        return info;
                    })
                    .collect(Collectors.toList());
            LOGGER.info("[getAllRoles] Returning {} roles", result.size());
            return result;
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve all roles", ex);
            throw new IAMException("Error retrieving roles", ex);
        }
    }

    @Override
    public Optional<RoleInfo> getRole(String departmentCode, String roleName) throws IAMException {
        LOGGER.info("[getRole] Input params: departmentCode={}, roleName={}", departmentCode, roleName);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            final String clientUUID = getClientInternalId(realmResource);
            try {
                RoleRepresentation role = realmResource.clients().get(clientUUID).roles().get(roleName).toRepresentation();
                LOGGER.info("[getRole] Role fetched: {}", role != null ? role.getName() : null);
                RoleInfo info = new RoleInfo();
                info.setName(Objects.requireNonNull(role).getName());
                info.setDescription(role.getDescription());
                info.setDepartmentCode(departmentCode);
                info.setStatus("ACTIVE");
                LOGGER.info("[getRole] RoleInfo built: {}", info);
                return Optional.of(info);
            } catch (Exception e) {
                LOGGER.info("[getRole] Role not found for departmentCode={}, roleName={}", departmentCode, roleName);
                return Optional.empty();
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve role '{}', departmentCode='{}'", roleName, departmentCode, ex);
            throw new IAMException("Error retrieving role", ex);
        }
    }

    @Override
    public List<UserIdentity> getAllUsers() throws IAMException {
        LOGGER.info("[getAllUsers] Called");
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            List<UserRepresentation> users = realmResource.users().list();
            LOGGER.info("[getAllUsers] Total users fetched: {}", users.size());
            List<UserIdentity> result = users.stream()
                    .map(user -> IGRPUserRepresentation.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .enable(user.isEnabled())
                            .emailVerified(user.isEmailVerified())
                            .build())
                    .collect(Collectors.toList());
            LOGGER.info("[getAllUsers] Returning {} users", result.size());
            return result;
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve all users", ex);
            throw new IAMException("Error retrieving users", ex);
        }
    }

    @Override
    public Map<String, Set<String>> getUserRoles(String username) throws IAMException {
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            UserRepresentation user = realmResource.users().search(username, true).stream().findFirst()
                    .orElseThrow(() -> new IAMException("User not found: " + username));

            List<GroupRepresentation> userGroups = realmResource.users().get(user.getId()).groups();

            return userGroups.stream()
                    .filter(this::isRoleGroup)
                    .collect(Collectors.groupingBy(
                            group -> extractParentDepartment(group.getPath()),
                            Collectors.mapping(GroupRepresentation::getName, Collectors.toSet())
                    ));
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve roles for user '{}'", username, ex);
            throw new IAMException("Error retrieving user roles", ex);
        }
    }

    @Override
    public Set<String> getRoleUsers(String departmentCode, String roleName) throws IAMException {
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());

            // FIXED: Use proper role lookup that handles hierarchical paths
            String roleGroupPath = findRoleGroupPath(realmResource, departmentCode, roleName);
            if (roleGroupPath == null) {
                return Set.of();
            }

            GroupRepresentation roleGroup = findGroupByPath(realmResource, roleGroupPath);
            if (roleGroup == null) {
                return Set.of();
            }

            List<UserRepresentation> users = realmResource.groups().group(roleGroup.getId()).members();
            return users.stream()
                    .map(UserRepresentation::getUsername)
                    .collect(Collectors.toSet());
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve users for role '{}'", roleName, ex);
            throw new IAMException("Error retrieving role users", ex);
        }
    }

    @Override
    public Map<String, Map<String, Set<String>>> getAllUserRoles() throws IAMException {
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            List<UserRepresentation> users = realmResource.users().list();

            Map<String, Map<String, Set<String>>> result = new HashMap<>();

            for (UserRepresentation user : users) {
                Map<String, Set<String>> userRoles = getUserRoles(user.getUsername());
                result.put(user.getUsername(), userRoles);
            }

            return result;
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve all user roles", ex);
            throw new IAMException("Error retrieving all user roles", ex);
        }
    }

    @Override
    public List<PermissionInfo> getAllPermissions() throws IAMException {
        LOGGER.info("[getAllPermissions] Called");
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);
            AuthorizationResource authorizationResource = realmResource.clients().get(clientId).authorization();
            List<PolicyRepresentation> policies = authorizationResource.policies().policies();
            LOGGER.info("[getAllPermissions] Total policies fetched: {}", policies.size());
            List<PermissionInfo> result = policies.stream()
                    .filter(policy -> policy.getType() != null && policy.getType().equals(PolicyType.ROLE.getName()))
                    .filter(policy -> policy.getName() != null && policy.getName().endsWith(POLICY_SUFFIX))
                    .map(policy -> {
                        String permissionName = policy.getName().replace(POLICY_SUFFIX, "");
                        PermissionInfo info = new PermissionInfo();
                        info.setName(permissionName);
                        info.setDescription(policy.getDescription());
                        info.setStatus("ACTIVE");
                        LOGGER.info("[getAllPermissions] PermissionInfo built: {}", info);
                        return info;
                    })
                    .collect(Collectors.toList());
            LOGGER.info("[getAllPermissions] Returning {} permissions", result.size());
            return result;
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve all permissions", ex);
            throw new IAMException("Error retrieving permissions", ex);
        }
    }

    @Override
    public Optional<PermissionInfo> getPermission(String permissionName) throws IAMException {
        LOGGER.info("[getPermission] Input param: permissionName={}", permissionName);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);
            AuthorizationResource authorizationResource = realmResource.clients().get(clientId).authorization();
            ResourcePermissionRepresentation permission = authorizationResource.permissions().resource().findByName(permissionName);
            LOGGER.info("[getPermission] Permission fetched: {}", permission != null ? permission.getName() : null);
            if (permission != null) {
                PermissionInfo info = new PermissionInfo();
                info.setName(permission.getName());
                info.setDescription(permission.getDescription());
                info.setStatus("ACTIVE");
                LOGGER.info("[getPermission] PermissionInfo built: {}", info);
                return Optional.of(info);
            }
            LOGGER.info("[getPermission] Permission not found: {}", permissionName);
            return Optional.empty();
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve permission '{}': {}", permissionName, ex.getMessage(), ex);
            throw new IAMException("Error retrieving permission", ex);
        }
    }


    @Override
    public void resetProvider() throws IAMException {
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);
            AuthorizationResource authorization = realmResource.clients().get(clientId).authorization();

            // Delete all user-role assignments
            List<UserRepresentation> users = realmResource.users().list();
            for (UserRepresentation user : users) {
                List<GroupRepresentation> userGroups = realmResource.users().get(user.getId()).groups();
                for (GroupRepresentation group : userGroups) {
                    realmResource.users().get(user.getId()).leaveGroup(group.getId());
                }
            }

            // Delete all roles
            List<RoleRepresentation> roles = realmResource.clients().get(clientId).roles().list();
            for (RoleRepresentation role : roles) {
                realmResource.clients().get(clientId).roles().deleteRole(role.getName());
            }

            // Delete all permissions (role-based policies)
            List<PolicyRepresentation> policies = authorization.policies().policies();
            for (PolicyRepresentation policy : policies) {
                if (policy.getType() != null && policy.getType().equals(PolicyType.ROLE.getName()) &&
                        policy.getName() != null && policy.getName().endsWith(POLICY_SUFFIX)) {
                    authorization.policies().policy(policy.getId()).remove();
                }
            }

            // Delete all other policies
            for (PolicyRepresentation policy : policies) {
                if (!policy.getType().equals(PolicyType.ROLE.getName()) ||
                        !policy.getName().endsWith(POLICY_SUFFIX)) {
                    authorization.policies().policy(policy.getId()).remove();
                }
            }

            // Delete all resources
            List<ResourceRepresentation> resources = authorization.resources().resources();
            for (ResourceRepresentation resource : resources) {
                authorization.resources().resource(resource.getId()).remove();
            }

            // Delete all groups (applications and departments)
            List<GroupRepresentation> groups = getAllGroupsRecursively(realmResource.groups());
            for (GroupRepresentation group : groups) {
                realmResource.groups().group(group.getId()).remove();
            }

            LOGGER.info("Provider reset completed successfully");
        } catch (Exception ex) {
            LOGGER.error("Failed to reset provider", ex);
            throw new IAMException("Error resetting provider", ex);
        }
    }

    @Override
    public boolean departmentExists(String departmentCode) throws IAMException {
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            GroupRepresentation group = findDepartmentByName(realmResource, departmentCode);
            return group != null;
        } catch (NotFoundException e) {
            return false;
        } catch (Exception ex) {
            LOGGER.error("Failed to check department existence '{}'", departmentCode, ex);
            throw new IAMException("Error checking department existence", ex);
        }
    }

    @Override
    public boolean applicationExists(String departmentCode, String applicationCode) throws IAMException {
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            // Use proper application path resolution
            String appPath = findApplicationPath(realmResource, departmentCode, applicationCode);
            return appPath != null;
        } catch(NotFoundException e) {
            return false;
        } catch (Exception ex) {
            LOGGER.error("Failed to check application existence '{}'", applicationCode, ex);
            throw new IAMException("Error checking application existence", ex);
        }
    }

    @Override
    public boolean roleExists(String departmentCode, String roleName) throws IAMException {
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            // Use proper role path resolution
            String rolePath = findRoleGroupPath(realmResource, departmentCode, roleName);
            return rolePath != null;
        } catch (NotFoundException e) {
            return false;
        } catch (Exception ex) {
            LOGGER.error("Failed to check role existence '{}'", roleName, ex);
            throw new IAMException("Error checking role existence", ex);
        }
    }

    @Override
    public boolean permissionExists(String permissionName) throws IAMException {
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);
            AuthorizationResource authorizationResource = realmResource.clients().get(clientId).authorization();

            ResourcePermissionRepresentation permission = authorizationResource.permissions().resource().findByName(permissionName);
            return permission != null;
        } catch (NotFoundException e) {
            return false;
        } catch (Exception ex) {
            LOGGER.error("Failed to check permission existence '{}'", permissionName, ex);
            throw new IAMException("Error checking permission existence", ex);
        }
    }

    @Override
    public boolean resourceExists(String resourceName) throws IAMException {
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);
            List<ResourceRepresentation> resources = realmResource.clients().get(clientId).authorization().resources().findByName(resourceName);
            return resources != null && !resources.isEmpty();
        } catch (NotFoundException e) {
            return false;
        } catch (Exception ex) {
            LOGGER.error("Failed to check resource existence '{}'", resourceName, ex);
            throw new IAMException("Error checking resource existence", ex);
        }
    }

    @Override
    public boolean protocolMapperExists(String name) throws IAMException {
        try(Keycloak keycloak = keycloakClientFactory.createClient()) {

            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);

            ClientResource client = realmResource.clients().get(clientId);

            List<ProtocolMapperRepresentation> protocols = client.getProtocolMappers().getMappers();

            return protocols.stream().map(ProtocolMapperRepresentation::getName)
                    .anyMatch(it -> it.equals(name));

        } catch (NotFoundException e) {
            return false;
        } catch (Exception ex) {
            LOGGER.error("Failed to check protocol mapper existence '{}'", name, ex);
            throw new IAMException("Error checking protocol mapper existence", ex);
        }
    }

    @Override
    public Set<String> getPermissionRoles(String permissionName) throws IAMException {
        LOGGER.info("[getPermissionRoles] Input param: permissionName={}", permissionName);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);
            String policyName = permissionName + POLICY_SUFFIX;
            AuthorizationResource authorizationResource = realmResource.clients().get(clientId).authorization();
            PolicyRepresentation policy = authorizationResource.policies().findByName(policyName);
            LOGGER.info("[getPermissionRoles] Policy fetched: {}", policy != null ? policy.getName() : null);
            if (policy != null && policy.getConfig() != null && policy.getConfig().containsKey("roles")) {
                String rolesJson = policy.getConfig().get("roles");
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> roles = mapper.readValue(rolesJson, new TypeReference<>() {});
                Set<String> result = roles.stream()
                        .map(role -> {
                            String roleId = (String) role.get("id");
                            return realmResource.clients().get(clientId).roles().get(roleId).toRepresentation().getName();
                        })
                        .collect(Collectors.toSet());
                LOGGER.info("[getPermissionRoles] Returning roles: {}", result);
                return result;
            }
            LOGGER.info("[getPermissionRoles] No roles found for permission: {}", permissionName);
            return Set.of();
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve roles for permission '{}': {}", permissionName, ex.getMessage(), ex);
            throw new IAMException("Error retrieving permission roles", ex);
        }
    }

    @Override
    public Set<String> getRolePermissions(String departmentCode, String roleName) throws IAMException {
        LOGGER.info("[getRolePermissions] Input params: departmentCode={}, roleName={}", departmentCode, roleName);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);
            AuthorizationResource authorizationResource = realmResource.clients().get(clientId).authorization();
            List<PolicyRepresentation> policies = authorizationResource.policies().policies();
            RoleRepresentation role = realmResource.clients().get(clientId).roles().get(roleName).toRepresentation();
            LOGGER.info("[getRolePermissions] Role fetched: {}", role != null ? role.getName() : null);
            Set<String> result = policies.stream()
                    .filter(policy -> policy.getType() != null && policy.getType().equals(PolicyType.ROLE.getName()))
                    .filter(policy -> policy.getName() != null && policy.getName().endsWith(POLICY_SUFFIX))
                    .filter(policy -> {
                        try {
                            if (policy.getConfig() != null && policy.getConfig().containsKey("roles")) {
                                String rolesJson = policy.getConfig().get("roles");
                                ObjectMapper mapper = new ObjectMapper();
                                List<Map<String, Object>> roles = mapper.readValue(rolesJson, new TypeReference<>() {});
                                return roles.stream().anyMatch(r -> Objects.requireNonNull(role).getId().equals(r.get("id")));
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .map(policy -> policy.getName().replace(POLICY_SUFFIX, ""))
                    .collect(Collectors.toSet());
            LOGGER.info("[getRolePermissions] Returning permissions: {}", result);
            return result;
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve permissions for role '{}': {}", roleName, ex.getMessage(), ex);
            throw new IAMException("Error retrieving role permissions", ex);
        }
    }

    @Override
    public Map<String, Set<String>> getAllRolePermissions() throws IAMException {
        LOGGER.info("[getAllRolePermissions] Called");
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);
            AuthorizationResource authorizationResource = realmResource.clients().get(clientId).authorization();
            Map<String, Set<String>> result = new HashMap<>();
            List<PolicyRepresentation> policies = authorizationResource.policies().policies();
            List<PolicyRepresentation> rolePolicies = policies.stream()
                    .filter(policy -> policy.getType() != null && policy.getType().equals(PolicyType.ROLE.getName()))
                    .filter(policy -> policy.getName() != null && policy.getName().endsWith(POLICY_SUFFIX))
                    .collect(Collectors.toList());
            LOGGER.info("[getAllRolePermissions] Total role policies: {}", rolePolicies.size());
            for (PolicyRepresentation policy : rolePolicies) {
                String permissionName = policy.getName().replace(POLICY_SUFFIX, "");
                Set<String> roles = getPermissionRoles(permissionName);
                result.put(permissionName, roles);
                LOGGER.info("[getAllRolePermissions] Permission: {}, Roles: {}", permissionName, roles);
            }
            LOGGER.info("[getAllRolePermissions] Returning all role permissions");
            return result;
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve all role permissions", ex);
            throw new IAMException("Error retrieving all role permissions", ex);
        }
    }

    @Override
    public List<ResourceInfo> getAllResources() throws IAMException {
        LOGGER.info("[getAllResources] Called");
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);
            List<ResourceRepresentation> resources = realmResource.clients().get(clientId).authorization().resources().resources();
            LOGGER.info("[getAllResources] Total resources fetched: {}", resources.size());
            List<ResourceInfo> result = resources.stream()
                    .map(resource -> {
                        ResourceInfo info = new ResourceInfo();
                        info.setName(resource.getName());
                        info.setDescription(resource.getDisplayName());
                        info.setUris(new ArrayList<>(resource.getUris()));
                        info.setScopes(resource.getScopes() != null ?
                                resource.getScopes().stream().map(ScopeRepresentation::getName).collect(Collectors.toList()) :
                                List.of());
                        info.setStatus("ACTIVE");
                        LOGGER.info("[getAllResources] ResourceInfo built: {}", info);
                        return info;
                    })
                    .collect(Collectors.toList());
            LOGGER.info("[getAllResources] Returning {} resources", result.size());
            return result;
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve all resources", ex);
            throw new IAMException("Error retrieving resources", ex);
        }
    }

    @Override
    public void createJwtRolesClaimMapper(String claimName, String claimDescription) throws IAMException {
        LOGGER.info("[createJwtRolesClaimMapper] Creating JWT roles claim mapper with claim name '{}' and description '{}'", claimName, claimDescription);

        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);

            ClientResource client = realmResource.clients().get(clientId);

            ProtocolMapperRepresentation protocol = new ProtocolMapperRepresentation();

            Map<String, String> config = new HashMap<>();

            config.put("claim.name", claimName);
            config.put("full.path", "true");
            config.put("id.token.claim", "true");
            config.put("access.token.claim", "true");
            config.put("userinfo.token.claim", "true");

            protocol.setConfig(config);
            protocol.setProtocol("openid-connect");
            protocol.setProtocolMapper("oidc-group-membership-mapper");
            protocol.setName(claimDescription);

            try (Response mapperResponse = client.getProtocolMappers().createMapper(protocol)) {
                LOGGER.info("[createJwtRolesClaimMapper] Mapper creation response status: {}", mapperResponse.getStatus());
                if(mapperResponse.getStatus() < 200 || mapperResponse.getStatus() >= 300) {
                    throw new IAMException("Error creating JWT roles claim mapper. HTTP Status: " + mapperResponse.getStatus());
                }
            }

            LOGGER.info("JWT Roles Claim Mapper '{}' created successfully", claimName);

        } catch (Exception ex) {
            LOGGER.error("Failed to create role claim mapper '{}'", claimName, ex);
            throw new IAMException("Error creating role claim mapper", ex);
        }
    }

    @Override
    public List<String> getAllClientProtocolMappers() throws IAMException {

        LOGGER.info("[getAllClientProtocolMappers] Fetching client protocol mappers...");

        try(Keycloak keycloak = keycloakClientFactory.createClient()) {

            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);

            ClientResource client = realmResource.clients().get(clientId);

            List<ProtocolMapperRepresentation> protocols = client.getProtocolMappers().getMappers();

            return protocols.stream().map(ProtocolMapperRepresentation::getName).collect(Collectors.toList());

        } catch (Exception ex) {
            LOGGER.error("Failed to fetch client protocol mappers", ex);
            throw new IAMException("Failed to fetch client protocol mappers", ex);
        }

    }

    @Override
    public Optional<ResourceInfo> getResource(String resourceName) throws IAMException {
        LOGGER.info("[getResource] Input param: resourceName={}", resourceName);
        try (Keycloak keycloak = keycloakClientFactory.createClient()) {
            RealmResource realmResource = keycloak.realm(keycloakProperties.getRealm());
            String clientId = getClientInternalId(realmResource);

            List<ResourceRepresentation> resources = realmResource.clients().get(clientId).authorization().resources().findByName(resourceName);
            LOGGER.info("[getResource] Resources fetched: {}", resources != null ? resources.size() : 0);
            if (resources != null && !resources.isEmpty()) {
                ResourceRepresentation resource = resources.getFirst();
                ResourceInfo info = new ResourceInfo();
                info.setName(resource.getName());
                info.setDescription(resource.getDisplayName());
                info.setUris(new ArrayList<>(resource.getUris()));
                info.setScopes(resource.getScopes() != null ?
                        resource.getScopes().stream().map(ScopeRepresentation::getName).collect(Collectors.toList()) :
                        List.of());
                info.setStatus("ACTIVE");
                LOGGER.info("[getResource] ResourceInfo built: {}", info);
                return Optional.of(info);
            }
            LOGGER.info("[getResource] Resource not found: {}", resourceName);
            return Optional.empty();
        } catch (Exception ex) {
            LOGGER.error("Failed to retrieve resource '{}': {}", resourceName, ex.getMessage(), ex);
            throw new IAMException("Error retrieving resource", ex);
        }
    }

    // =====================================================
    // Helper Methods for Group Hierarchy Management
    // =====================================================

    /**
     * Enhanced method to find groups by either full path or name anywhere in hierarchy
     */
    private GroupRepresentation findGroupByPath(RealmResource realmResource, String groupIdentifier) {
        try {
            LOGGER.info("[findGroupByPath] Looking for group by identifier: {}", groupIdentifier);

            // First, check if it's a full path (starts with /)
            if (groupIdentifier.startsWith(GROUP_PATH_SEPARATOR)) {
                var group = realmResource.getGroupByPath(groupIdentifier);
                LOGGER.info("[findGroupByPath] Found group by full path: {} -> {}", groupIdentifier, group != null ? group.getName() : null);
                return group;
            }

            // If it's not a full path, search recursively by name
            List<GroupRepresentation> allGroups = getAllGroupsRecursively(realmResource.groups());
            LOGGER.info("[findGroupByPath] Total groups fetched for recursive search: {}", allGroups.size());

            GroupRepresentation foundGroup = allGroups.stream()
                    .filter(group -> {
                        boolean nameMatch = groupIdentifier.equals(group.getName());
                        boolean pathMatch = groupIdentifier.equals(extractLastPathSegment(group.getPath()));

                        LOGGER.info("[findGroupByPath] Checking group: {} (path: {}) - nameMatch: {}, pathMatch: {}",
                                group.getName(), group.getPath(), nameMatch, pathMatch);

                        return nameMatch || pathMatch;
                    })
                    .findFirst()
                    .orElse(null);

            LOGGER.info("[findGroupByPath] Recursive search result: {}", foundGroup != null ? foundGroup.getName() : null);
            return foundGroup;

        } catch (NotFoundException e) {
            LOGGER.info("[findGroupByPath] Group not found by direct path lookup: {}", groupIdentifier);
            return null;
        } catch (Exception ex) {
            LOGGER.error("[findGroupByPath] Error finding group: {}", groupIdentifier, ex);
            return null;
        }
    }

    /**
     * Recursively gets all groups including subgroups
     */
    private List<GroupRepresentation> getAllGroupsRecursively(GroupsResource groupsResource) {
        return getAllGroupsRecursively(groupsResource.query("true"));
    }

    private List<GroupRepresentation> getAllGroupsRecursively(List<GroupRepresentation> groups) {
        List<GroupRepresentation> allGroups = new ArrayList<>(groups);
        for (GroupRepresentation group : groups) {
            LOGGER.info("[getAllGroupsRecursively] Processing group: {} with path: {}", group.getName(), group.getPath());

            List<GroupRepresentation> subgroups = group.getSubGroups();

            if (subgroups != null && !subgroups.isEmpty()) {
                LOGGER.info("[getAllGroupsRecursively] Group {} has {} subgroups", group.getName(), subgroups.size());
                allGroups.addAll(getAllGroupsRecursively(subgroups));
            }
        }
        return allGroups;
    }

    private GroupRepresentation findRoleParentGroup(RealmResource realmResource, GroupRepresentation departmentGroup) {
        GroupsResource groupsResource = realmResource.groups();
        List<GroupRepresentation> allGroups = getAllGroupsRecursively(List.of(departmentGroup));

        // Rest of the method remains the same...
        return allGroups.stream()
                .filter(group -> {
                    LOGGER.info("[findRoleParentGroup] Checking group: {} with path: {}", group.getName(), group.getPath());
                    return isRoleGroup(group) &&
                            group.getPath().startsWith(departmentGroup.getPath());
                })
                .findFirst().orElse(departmentGroup);

    }

    /**
     * Determines if a group is an application group
     */
    private boolean isApplicationGroup(GroupRepresentation group, List<GroupRepresentation> allGroups) {
        Map<String, List<String>> attributes = group.getAttributes();
        if (attributes != null && attributes.containsKey(ATTR_TYPE)) {
            List<String> values = attributes.get(ATTR_TYPE);
            return values != null && values.contains(TYPE_APPLICATION);
        }

        // fallback: old behavior based on hierarchy if no attribute present
        String parentPath = extractParentPath(group.getPath());
        GroupRepresentation parent = allGroups.stream()
                .filter(g -> g.getPath().equals(parentPath))
                .findFirst()
                .orElse(null);

        return parent != null && !isRoleGroup(group) && group.getName().startsWith(APP_NAME_PREFIX);
    }

    /**
     * Determines if a group is a role group - simplified version
     * Since roles can have child roles, we can't rely on leaf node status
     */
    private boolean isRoleGroup(GroupRepresentation group) {

        Map<String, List<String>> attributes = group.getAttributes();
        if (attributes != null && attributes.containsKey(ATTR_TYPE)) {
            List<String> values = attributes.get(ATTR_TYPE);
            return values != null && values.contains(TYPE_ROLE);
        }

        // fallback: old behavior based on naming convention if no attribute present
        // For now, just return true for any group that's not at the root level
        // You might want to add more specific logic based on your naming conventions
        return group.getPath() != null && group.getPath().split("/").length > 2 && group.getName().contains(".");
    }

    /**
     * Finds the full path of an application within a department hierarchy
     */
    private String findApplicationPath(RealmResource realmResource, String departmentCode, String applicationCode) {
        GroupRepresentation departmentGroup = findGroupByPath(realmResource, departmentCode);
        if (departmentGroup == null) {
            LOGGER.info("[findApplicationPath] Department group not found for code: {}", departmentCode);
            return null;
        }

        GroupsResource groupsResource = realmResource.groups();
        List<GroupRepresentation> allGroups = getAllGroupsRecursively(List.of(departmentGroup));

        String normalizedAppName = normalizeAppName(applicationCode);
        return allGroups.stream()
                .filter(group -> {
                    LOGGER.info("[findApplicationPath] Checking group: {} with path: {} for application: {}",
                            group.getName(), group.getPath(), normalizedAppName);
                    // Check if this is the application group by name and ensure it's under the correct department
                    return normalizedAppName.equals(group.getName()) &&
                            group.getPath() != null &&
                            group.getPath().startsWith("/" + departmentCode) &&
                            isApplicationGroup(group, allGroups);
                })
                .map(GroupRepresentation::getPath)
                .findFirst()
                .orElse(null);
    }

    /**
     * Enhanced role group path finder that properly handles hierarchy
     */
    private String findRoleGroupPath(RealmResource realmResource, String departmentCode, String roleName) {
        GroupRepresentation departmentGroup = findGroupByPath(realmResource, departmentCode);
        if (departmentGroup == null) {
            LOGGER.info("[findRoleGroupPath] Department group not found for code: {}", departmentCode);
            return null;
        }

        GroupsResource groupsResource = realmResource.groups();
        List<GroupRepresentation> allGroups = getAllGroupsRecursively(List.of(departmentGroup));

        return allGroups.stream()
                .filter(group -> {
                    LOGGER.info("[findRoleGroupPath] Checking group: {} with path: {} for role: {}",
                            group.getName(), group.getPath(), roleName);
                    // Direct match by name and ensure it's under the correct department and is a role group
                    return roleName.equals(group.getName()) &&
                            group.getPath() != null &&
                            group.getPath().startsWith("/" + departmentCode) &&
                            isRoleGroup(group);
                })
                .map(GroupRepresentation::getPath)
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a department by code anywhere in the hierarchy
     */
    private GroupRepresentation findDepartmentByName(RealmResource realmResource, String departmentCode) {
        LOGGER.info("[findDepartmentByName] Looking for department: {}", departmentCode);

        List<GroupRepresentation> allGroups = getAllGroupsRecursively(realmResource.groups());

        return allGroups.stream()
                .filter(group -> {
                    // Check if this is a department group (not application, not role)
                    boolean isDepartment = isDepartmentGroup(group);
                    boolean nameMatch = departmentCode.equals(group.getName());

                    LOGGER.info("[findDepartmentByName] Checking group: {} (path: {}) - isDepartment: {}, nameMatch: {}",
                            group.getName(), group.getPath(), isDepartment, nameMatch);

                    return isDepartment && nameMatch;
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Determines if a group is a department group
     */
    private boolean isDepartmentGroup(GroupRepresentation group) {
        Map<String, List<String>> attributes = group.getAttributes();
        if (attributes != null && attributes.containsKey(ATTR_TYPE)) {
            List<String> values = attributes.get(ATTR_TYPE);
            return values != null && values.contains(TYPE_DEPARTMENT);
        }

        // Fallback: not an application and not a role group
        return !isApplicationGroup(group, List.of()) && !isRoleGroup(group);
    }

    /**
     * Builds the full group path from a group representation
     */
    private String buildGroupPath(GroupRepresentation group) {
        return group.getPath() != null ? group.getPath() : GROUP_PATH_SEPARATOR + group.getName();
    }

    /**
     * Extracts parent path from a group path
     */
    private String extractParentPath(String path) {
        if (path == null || path.isEmpty() || path.equals(GROUP_PATH_SEPARATOR)) {
            return null;
        }
        int lastSeparator = path.lastIndexOf(GROUP_PATH_SEPARATOR);
        return lastSeparator > 0 ? path.substring(0, lastSeparator) : GROUP_PATH_SEPARATOR;
    }

    private String extractParentDepartment(String path) {
        if (path == null || path.isEmpty() || !path.contains(GROUP_PATH_SEPARATOR)) {
            return null;
        }
        String[] parts = path.split(GROUP_PATH_SEPARATOR);
        return parts.length > 1 ? parts[1] : null;
    }

    private String extractDepartmentFromRoleName(String roleName) {
        if (roleName != null && roleName.contains(".")) {
            String[] parts = roleName.split("\\.");
            return parts.length > 0 ? parts[0] : null;
        }
        return null;
    }

    private String normalizeAppName(String appName) {
        if (appName == null || appName.isEmpty()) {
            return appName;
        }
        return APP_NAME_PREFIX + appName;
    }

    private String removeAppPrefix(String appName) {
        if (appName != null && appName.startsWith(APP_NAME_PREFIX)) {
            return appName.substring(4);
        }
        return appName;
    }

    /**
     * Extracts the last segment from a group path (e.g., "/A/B/C" -> "C")
     */
    private String extractLastPathSegment(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String[] segments = path.split(GROUP_PATH_SEPARATOR);
        return segments.length > 0 ? segments[segments.length - 1] : null;
    }

    private String getClientInternalId(RealmResource realmResource) throws IAMException {
        return realmResource.clients()
                .findByClientId(keycloakProperties.getClientId())
                .stream()
                .findFirst()
                .map(ClientRepresentation::getId)
                .orElseThrow(() -> new IAMException("Client not found: " + keycloakProperties.getClientId()));
    }

}
