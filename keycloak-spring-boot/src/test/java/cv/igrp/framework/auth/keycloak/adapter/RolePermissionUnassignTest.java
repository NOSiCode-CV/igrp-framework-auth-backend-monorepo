package cv.igrp.framework.auth.keycloak.adapter;


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

import java.util.*;

import static cv.igrp.framework.auth.keycloak.adapter.KeycloakAdapter.POLICY_SUFFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Disabled
class RolePermissionUnassignTest {

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

        when(clientResource.authorization()).thenReturn(authorizationResource);
        when(authorizationResource.policies()).thenReturn(policiesResource);
    }

    @Nested
    class PermissionFromRoles {

        @Test
        @Disabled
        void shouldUnassignPermissionFromRolesSuccessfully() throws Exception {
            // GIVEN
            final String roleIdToRemove = "1234";
            final String roleIdKeep = "4321";

            Map<String, Object> roleToRemove = Map.of("id", roleIdToRemove, "required", true);
            Map<String, Object> roleToKeep = Map.of("id", roleIdKeep, "required", true);

            RoleRepresentation role1 = new RoleRepresentation();
            role1.setId(roleIdToRemove);
            role1.setName("role_1");
            RoleRepresentation role2 = new RoleRepresentation();
            role2.setId(roleIdKeep);
            role2.setName("role_2");

            PolicyRepresentation policy = new PolicyRepresentation();
            policy.setId(policyId);
            policy.setConfig(new HashMap<>(Map.of("roles", new ObjectMapper().writeValueAsString(List.of(roleToRemove, roleToKeep)))));

            when(policiesResource.findByName(policyName)).thenReturn(policy);
            when(policiesResource.policy(policy.getId())).thenReturn(policyResource);

            when(clientResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get(role1.getName())).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenReturn(role1);

            // WHEN
            keycloakAdapter.unassignPermissionFromRoles(permissionName, Set.of(role1.getName()));

            // THEN
            verify(policyResource).update(argThat(updated -> {
                try {
                    String json = updated.getConfig().get("roles");
                    List<Map<String, Object>> roles = new ObjectMapper().readValue(json, new TypeReference<>() {
                    });
                    return roles.size() == 1 && roles.get(0).get("id").equals(roleIdKeep);
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        @Test
        @Disabled
        void shouldThrowIAMExceptionWhenPolicyNotFound() {
            // GIVEN
            final String roleName = "role_1";

            when(policiesResource.findByName(policyName)).thenThrow(new RuntimeException("Permission not found"));

            // WHEN / THEN
            IAMException ex = assertThrows(IAMException.class, () ->
                    keycloakAdapter.unassignPermissionFromRoles(permissionName, Set.of(roleName))
            );
            assertEquals("Error unassigning permission from roles", ex.getMessage());
        }

        @Test
        @Disabled
        void shouldNotModifyPolicyIfNoRolesMatch() throws Exception {
            // GIVEN
            final String roleId = "1234";
            final String roleName = "role_1";
            Map<String, Object> existing = Map.of("id", roleId, "required", true);
            RoleRepresentation role = new RoleRepresentation();
            role.setId("4321");

            PolicyRepresentation policy = new PolicyRepresentation();
            policy.setId("999");
            policy.setConfig(new HashMap<>(Map.of("roles", new ObjectMapper().writeValueAsString(List.of(existing)))));

            when(policiesResource.findByName(policyName)).thenReturn(policy);
            when(policiesResource.policy(policy.getId())).thenReturn(policyResource);
            when(roleResource.toRepresentation()).thenReturn(role);
            when(rolesResource.get(roleName)).thenReturn(roleResource);

            when(clientResource.roles()).thenReturn(rolesResource);

            // WHEN
            keycloakAdapter.unassignPermissionFromRoles(permissionName, Set.of(roleName));

            // THEN
            verify(policyResource).update(argThat(updated -> {
                try {
                    String json = updated.getConfig().get("roles");
                    List<Map<String, Object>> roles = new ObjectMapper().readValue(json, new TypeReference<>() {
                    });
                    return roles.size() == 1 && roles.get(0).get("id").equals(roleId);
                } catch (Exception e) {
                    return false;
                }
            }));
        }

    }

    @Nested
    class PermissionsFromRole {

        @Test
        @Disabled
        void shouldUnassignPermissionsFromRoleSuccessfully() throws Exception {
            // GIVEN
            final String roleId = "1234";
            final String roleName = "role_1";
            Set<String> permissions = Set.of("edit_invoice", "read_invoice");

            Map<String, Object> roleEntry = Map.of("id", roleId, "required", true);
            List<Map<String, Object>> policyRoles = new ArrayList<>(List.of(roleEntry));

            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setId(roleId);

            when(clientResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get(roleName)).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenReturn(roleRepresentation);

            for (String permission : permissions) {
                String policyName = permission + POLICY_SUFFIX;
                PolicyRepresentation policy = new PolicyRepresentation();
                policy.setId(policyId);
                policy.setConfig(new HashMap<>(Map.of("roles", new ObjectMapper().writeValueAsString(policyRoles))));

                when(policiesResource.findByName(policyName)).thenReturn(policy);
                when(policiesResource.policy(policy.getId())).thenReturn(policyResource);
            }

            // WHEN
            keycloakAdapter.unassignPermissionsFromRole(permissions, roleName);

            verify(policyResource, times(2)).update(argThat(updated -> {
                try {
                    String json = updated.getConfig().get("roles");
                    List<Map<String, Object>> roles = new ObjectMapper().readValue(json, new TypeReference<>() {
                    });
                    return roles.isEmpty();
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        @Test
        @Disabled
        void shouldNotUpdatePolicyIfRoleNotAssigned() throws Exception {
            // GIVEN
            final String roleId = "1234";
            final String roleName = "role_2";
            final String permission = "read_invoice";

            RoleRepresentation role = new RoleRepresentation();
            role.setId(roleId);

            Map<String, Object> otherRole = Map.of("id", "4321", "required", true);
            List<Map<String, Object>> roles = List.of(otherRole);

            PolicyRepresentation policy = new PolicyRepresentation();
            policy.setId(policyId);
            policy.setConfig(Map.of("roles", new ObjectMapper().writeValueAsString(roles)));

            when(clientResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get(roleName)).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenReturn(role);
            when(policiesResource.findByName(permission + POLICY_SUFFIX)).thenReturn(policy);

            // WHEN
            keycloakAdapter.unassignPermissionsFromRole(Set.of(permission), roleName);

            // THEN
            verify(policyResource, never()).update(any());
        }

        @Test
        @Disabled
        void shouldThrowIAMExceptionWhenPolicyNotFound() {
            // GIVEN
            final String roleName = "dummy-name";
            final String roleId = "dummy-id";
            when(clientResource.authorization()).thenReturn(authorizationResource);
            when(authorizationResource.policies()).thenReturn(policiesResource);
            when(policiesResource.findByName(policyName)).thenThrow(new RuntimeException("Policy not found"));

            when(clientResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get(roleName)).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenReturn(roleRepresentation);
            when(roleRepresentation.getId()).thenReturn(roleId);

            // WHEN / THEN
            IAMException ex = assertThrows(IAMException.class, () -> {
                keycloakAdapter.unassignPermissionsFromRole(Set.of("read_invoice"), "role_1");
            });

            assertEquals("Error unassigning permissions from role", ex.getMessage());
        }

    }

}
