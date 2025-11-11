package cv.igrp.framework.auth.keycloak.adapter;

import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.keycloak.client.KeycloakClientFactory;
import cv.igrp.framework.auth.keycloak.config.KeycloakProperties;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.GroupRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DepartmentManagementTest {

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
    private GroupResource groupResource;

    @Mock
    private Response response;

    @BeforeEach
    void setUp() {
        when(keycloakClientFactory.createClient()).thenReturn(keycloak);
        when(keycloakProperties.getRealm()).thenReturn("igrp_iam");
        when(keycloak.realm(anyString())).thenReturn(realmResource);
    }

    @Test
    void shouldCreateDepartmentSuccessfully() throws IAMException {
        String departmentName = "dept1";
        String parentDepartment = "parentDept";

        // Mock parent group with a non-null ID
        GroupRepresentation parentGroup = new GroupRepresentation();
        parentGroup.setId("parent-id-123");

        when(realmResource.groups()).thenReturn(groupsResource);
        when(realmResource.getGroupByPath(parentDepartment)).thenReturn(parentGroup);
        when(groupsResource.group("parent-id-123")).thenReturn(groupResource);
        when(groupResource.subGroup(any(GroupRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);

        keycloakAdapter.createDepartment(departmentName, parentDepartment);

        verify(groupResource).subGroup(any(GroupRepresentation.class));
        verify(response).close();
    }

    @Test
    void shouldCreateRootDepartmentSuccessfully() throws IAMException {
        String departmentName = "dept1";

        when(realmResource.groups()).thenReturn(groupsResource);
        when(groupsResource.add(any(GroupRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);

        keycloakAdapter.createDepartment(departmentName, null);

        verify(groupsResource).add(any(GroupRepresentation.class));
        verify(response).close();
    }

    @Test
    void shouldFailToCreateDepartment() {
        String departmentName = "dept1";
        String parentDepartment = "parentDept";

        // Mock parent group with a non-null ID
        GroupRepresentation parentGroup = new GroupRepresentation();
        parentGroup.setId("parent-id-123");

        when(realmResource.groups()).thenReturn(groupsResource);
        when(realmResource.getGroupByPath(parentDepartment)).thenReturn(parentGroup);
        when(groupsResource.group("parent-id-123")).thenReturn(groupResource);
        when(groupResource.subGroup(any(GroupRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(500);

        IAMException thrown = assertThrows(IAMException.class, () ->
                keycloakAdapter.createDepartment(departmentName, parentDepartment)
        );

        // Ensure the message matches what your method throws
        assertEquals("Error creating department", thrown.getMessage());
    }

    @Test
    void shouldUpdateDepartmentSuccessfully() throws IAMException {
        String oldName = "dept1";
        String newName = "dept1_new";

        GroupRepresentation group = new GroupRepresentation();
        group.setId("888");
        group.setName(oldName);

        when(realmResource.getGroupByPath(oldName)).thenReturn(group);
        when(realmResource.groups()).thenReturn(groupsResource);
        when(groupsResource.group("888")).thenReturn(groupResource);
        when(groupResource.toRepresentation()).thenReturn(group);

        keycloakAdapter.updateDepartment(oldName, newName);

        verify(groupResource).update(argThat(rep -> rep.getName().equals(newName)));
    }

    @Test
    void shouldDeleteDepartmentSuccessfully() throws IAMException {
        String departmentName = "dept1";

        GroupRepresentation group = new GroupRepresentation();
        group.setId("888");
        group.setName(departmentName);

        when(realmResource.getGroupByPath(departmentName)).thenReturn(group);
        when(realmResource.groups()).thenReturn(groupsResource);
        when(groupsResource.group("888")).thenReturn(groupResource);

        keycloakAdapter.deleteDepartment(departmentName);

        verify(groupResource).remove();
    }
}