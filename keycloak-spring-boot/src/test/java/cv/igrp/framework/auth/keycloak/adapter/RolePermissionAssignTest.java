package cv.igrp.framework.auth.keycloak.adapter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.keycloak.client.KeycloakClientFactory;
import cv.igrp.framework.auth.keycloak.config.KeycloakProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Disabled
class RolePermissionAssignTest {

    @Mock
    private KeycloakClientFactory keycloakClientFactory;
    @Mock
    private Keycloak keycloak;
    @Mock
    private RealmResource realmResource;
    @Mock
    private ClientsResource clientsResource;
    @Mock
    private ClientResource clientResource;
    @Mock
    private AuthorizationResource authorizationResource;
    @Mock
    private PoliciesResource policiesResource;
    @Mock
    private PolicyResource policyResource;
    @Mock
    private RolesResource rolesResource;
    @Mock
    private RoleResource roleResource;
    @Mock
    private RoleRepresentation roleRepresentation;

    @Mock
    private KeycloakProperties keycloakProperties;

    @InjectMocks
    private KeycloakAdapter keycloakAdapter;

    private final String realm = "igrp_iam";
    private final String clientId = "igrp";
    private final String clientUUID = "1234";
    private final String permissionName = "create_user";
    private final String policyId = "policy_id_1";
    private final String policyName = permissionName + "_p";
    private final String roleName = "admin";
    private final String roleId = "role-id-999";

    @BeforeEach
    void setup() {
        when(keycloakClientFactory.createClient()).thenReturn(keycloak);
        when(keycloakProperties.getRealm()).thenReturn(realm);
        when(keycloakProperties.getClientId()).thenReturn(clientId);
        when(keycloak.realm(realm)).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);

        // Client representation setup
        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setClientId(clientId);
        clientRepresentation.setId(clientUUID);
        when(clientsResource.findByClientId(clientId)).thenReturn(List.of(clientRepresentation));
        when(clientsResource.get(clientUUID)).thenReturn(clientResource);
    }

    @Nested
    class PermissionToRoles {

        @Test
        @Disabled
        void shouldAssignPermissionToRolesSuccessfully() throws Exception {
            // GIVEN
            PolicyRepresentation policy = new PolicyRepresentation();
            policy.setId(policyId);
            policy.setName(policyName);
            policy.setConfig(new HashMap<>());

            when(clientResource.authorization()).thenReturn(authorizationResource);
            when(authorizationResource.policies()).thenReturn(policiesResource);

            when(policiesResource.findByName(policyName)).thenReturn(policy);

            when(clientResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get(roleName)).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenReturn(roleRepresentation);
            when(roleRepresentation.getId()).thenReturn(roleId);

            when(policiesResource.policy(policyId)).thenReturn(policyResource);

            // WHEN
            keycloakAdapter.assignPermissionToRoles(permissionName, Set.of(roleName));

            // THEN
            verify(policyResource).update(argThat(updatedPolicy ->
                    updatedPolicy.getConfig().get("roles").contains(roleId)
            ));
        }

        @Test
        @Disabled
        void shouldThrowExceptionWhenRoleNotFound() {
            // GIVEN
            PolicyRepresentation policy = new PolicyRepresentation();
            policy.setId(policyId);
            policy.setName(policyName);
            policy.setConfig(new HashMap<>());

            when(clientResource.authorization()).thenReturn(authorizationResource);
            when(authorizationResource.policies()).thenReturn(policiesResource);
            when(policiesResource.findByName(policyName)).thenReturn(policy);

            when(clientResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get(roleName)).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenThrow(new RuntimeException("Role not found"));

            // WHEN / THEN
            IAMException ex = assertThrows(IAMException.class,
                    () -> keycloakAdapter.assignPermissionToRoles(permissionName, Set.of(roleName))
            );

            assertEquals("Error assigning permission to roles", ex.getMessage());
            verify(roleResource).toRepresentation();
        }

    }

    @Nested
    class PermissionsToRole {

        @Test
        @Disabled
        void shouldAssignPermissionsToRoleWhenNotAlreadyAssigned() throws Exception {
            // GIVEN
            RoleRepresentation role = new RoleRepresentation();
            role.setId(roleId);
            role.setName(roleName);

            PolicyRepresentation policy = new PolicyRepresentation();
            policy.setId(policyId);
            policy.setName(policyName);
            policy.setConfig(new HashMap<>(Map.of("roles", "[]")));

            when(roleResource.toRepresentation()).thenReturn(role);
            when(rolesResource.get(roleName)).thenReturn(roleResource);
            when(clientResource.roles()).thenReturn(rolesResource);
            when(clientResource.authorization()).thenReturn(authorizationResource);
            when(authorizationResource.policies()).thenReturn(policiesResource);
            when(policiesResource.findByName(policyName)).thenReturn(policy);
            when(policiesResource.policy(policy.getId())).thenReturn(policyResource);

            // WHEN
            keycloakAdapter.assignPermissionsToRole(Set.of(permissionName), roleName);

            // THEN
            verify(policyResource).update(argThat(updated -> {
                try {
                    String json = updated.getConfig().get("roles");
                    List<Map<String, Object>> roles = new ObjectMapper().readValue(json, new TypeReference<>() {
                    });
                    return roles.size() == 1 && roles.get(0).get("id").equals(roleId);
                } catch (JsonProcessingException e) {
                    return false;
                }
            }));
        }

        @Test
        @Disabled
        void shouldNotAssignPermissionIfRoleAlreadyExistsInPolicy() throws Exception {
            // GIVEN
            RoleRepresentation role = new RoleRepresentation();
            role.setId(roleId);
            role.setName(roleName);

            Map<String, Object> existingEntry = new HashMap<>();
            existingEntry.put("id", roleId);
            existingEntry.put("required", true);

            PolicyRepresentation policy = new PolicyRepresentation();
            policy.setId(policyId);
            policy.setName(policyName);
            policy.setConfig(Map.of("roles", new ObjectMapper().writeValueAsString(List.of(existingEntry))));

            when(roleResource.toRepresentation()).thenReturn(role);
            when(rolesResource.get(roleName)).thenReturn(roleResource);
            when(clientResource.roles()).thenReturn(rolesResource);
            when(clientResource.authorization()).thenReturn(authorizationResource);
            when(authorizationResource.policies()).thenReturn(policiesResource);
            when(policiesResource.findByName(policyName)).thenReturn(policy);

            // WHEN
            keycloakAdapter.assignPermissionsToRole(Set.of(permissionName), roleName);

            // THEN
            verify(policyResource, never()).update(any());
        }

        @Test
        @Disabled
        void shouldThrowIAMExceptionWhenPolicyNotFound() {
            // GIVEN
            when(clientResource.authorization()).thenReturn(authorizationResource);
            when(authorizationResource.policies()).thenReturn(policiesResource);

            when(clientResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get(roleName)).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenReturn(roleRepresentation);
            when(roleRepresentation.getId()).thenReturn(roleId);
            when(policiesResource.findByName(policyName)).thenThrow(new RuntimeException("Policy not found"));

            // WHEN / THEN
            IAMException ex = assertThrows(IAMException.class,
                    () -> keycloakAdapter.assignPermissionsToRole(Set.of(permissionName), roleName)
            );

            assertEquals("Error assigning permissions to role", ex.getMessage());
        }

    }


}

