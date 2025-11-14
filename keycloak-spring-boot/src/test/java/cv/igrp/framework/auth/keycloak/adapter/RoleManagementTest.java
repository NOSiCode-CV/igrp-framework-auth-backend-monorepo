package cv.igrp.framework.auth.keycloak.adapter;

import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.keycloak.client.KeycloakClientFactory;
import cv.igrp.framework.auth.keycloak.config.KeycloakProperties;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Disabled
public class RoleManagementTest {

    @InjectMocks
    private KeycloakAdapter keycloakAdapter;

    @Mock
    private KeycloakClientFactory keycloakClientFactory;

    @Mock
    private KeycloakProperties keycloakProperties;

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    @Mock
    private GroupsResource groupsResource;

    @Mock
    private GroupResource departmentGroupResource;

    @Mock
    private GroupResource roleGroupResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private RoleResource roleResource;

    @Mock
    private Response response;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @Mock
    private ClientsResource clientsResource;

    @Mock
    private ClientResource clientResource;

    @BeforeEach
    void setUp() {
        when(keycloakClientFactory.createClient()).thenReturn(keycloak);
        when(keycloakProperties.getRealm()).thenReturn("igrp_iam");
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);
    }

    @Test
    void shouldCreateRoleSuccessfully() throws IAMException {
        String departmentName = "dept1";
        String roleName = "role1";
        String groupPath = departmentName + "/" + roleName;

        GroupRepresentation departmentGroup = new GroupRepresentation();
        departmentGroup.setId("1");
        departmentGroup.setName(departmentName);

        GroupRepresentation roleGroup = new GroupRepresentation();
        roleGroup.setId("2");
        roleGroup.setName(roleName);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("igrp");
        clientRep.setId("1234");

        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setId("999");
        roleRep.setName(roleName);

        when(keycloakProperties.getClientId()).thenReturn("igrp");
        when(clientsResource.findByClientId("igrp")).thenReturn(List.of(clientRep));
        when(realmResource.groups()).thenReturn(groupsResource);
        when(realmResource.getGroupByPath(departmentName)).thenReturn(departmentGroup);
        when(groupsResource.group("1")).thenReturn(departmentGroupResource);
        when(departmentGroupResource.subGroup(any(GroupRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);

        when(clientsResource.get("1234")).thenReturn(clientResource);
        when(clientResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(roleName)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRep);

        when(realmResource.getGroupByPath(groupPath)).thenReturn(roleGroup);
        when(groupsResource.group("2")).thenReturn(roleGroupResource);
        when(roleGroupResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.clientLevel("1234")).thenReturn(roleScopeResource);

        keycloakAdapter.createRole(departmentName, roleName);

        verify(departmentGroupResource).subGroup(any(GroupRepresentation.class));
        verify(rolesResource).create(any(RoleRepresentation.class));
        verify(roleScopeResource).add(anyList());
        verify(response).close();
    }

    @Test
    void shouldFailToCreateRole() {
        String departmentName = "dept1";
        String roleName = "role1";

        GroupRepresentation departmentGroup = new GroupRepresentation();
        departmentGroup.setId("1");
        departmentGroup.setName(departmentName);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("igrp");
        clientRep.setId("1234");

        when(keycloakProperties.getClientId()).thenReturn("igrp");
        when(clientsResource.findByClientId("igrp")).thenReturn(List.of(clientRep));
        when(realmResource.groups()).thenReturn(groupsResource);
        when(realmResource.getGroupByPath(departmentName)).thenReturn(departmentGroup);
        when(groupsResource.group("1")).thenReturn(departmentGroupResource);
        when(departmentGroupResource.subGroup(any(GroupRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(500);

        IAMException thrown = assertThrows(IAMException.class, () ->
                keycloakAdapter.createRole(departmentName, roleName)
        );

        assertEquals("Error creating role", thrown.getMessage());
        verify(response).close();
    }

    @Test
    void shouldDeleteRoleSuccessfully() throws IAMException {
        String departmentName = "dept1";
        String roleName = "role1";
        String groupPath = departmentName + "/" + roleName;

        // Mock the role group with a non-null ID
        GroupRepresentation roleGroup = new GroupRepresentation();
        roleGroup.setId("999");
        roleGroup.setName(roleName);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("igrp");
        clientRep.setId("1234");

        when(keycloakProperties.getClientId()).thenReturn("igrp");
        when(clientsResource.findByClientId("igrp")).thenReturn(List.of(clientRep));
        when(clientsResource.get("1234")).thenReturn(clientResource);
        when(clientResource.roles()).thenReturn(rolesResource);

        when(realmResource.getGroupByPath(groupPath)).thenReturn(roleGroup);
        when(realmResource.groups()).thenReturn(groupsResource);
        when(groupsResource.group("999")).thenReturn(roleGroupResource);

        keycloakAdapter.deleteRole(departmentName, roleName);

        verify(roleGroupResource).remove();
        verify(rolesResource).deleteRole(roleName);
    }

    @Test
    void shouldUpdateRoleSuccessfully() throws IAMException {
        String departmentName = "dept1";
        String oldName = "role1";
        String newName = "role1_new";
        String oldGroupPath = departmentName + "/" + oldName;

        GroupRepresentation roleGroup = new GroupRepresentation();
        roleGroup.setId("999");
        roleGroup.setName(oldName);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("igrp");
        clientRep.setId("1234");

        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setId("999");
        roleRep.setName(oldName);

        when(keycloakProperties.getClientId()).thenReturn("igrp");
        when(clientsResource.findByClientId("igrp")).thenReturn(List.of(clientRep));
        when(clientsResource.get("1234")).thenReturn(clientResource);
        when(clientResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(oldName)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRep);

        when(realmResource.getGroupByPath(oldGroupPath)).thenReturn(roleGroup);
        when(realmResource.groups()).thenReturn(groupsResource);
        when(groupsResource.group("999")).thenReturn(roleGroupResource);
        when(roleGroupResource.toRepresentation()).thenReturn(roleGroup);

        keycloakAdapter.updateRole(departmentName, oldName, newName);

        verify(roleResource).update(argThat(rep -> rep.getName().equals(newName)));
        verify(roleGroupResource).update(argThat(rep -> rep.getName().equals(newName)));
    }
}