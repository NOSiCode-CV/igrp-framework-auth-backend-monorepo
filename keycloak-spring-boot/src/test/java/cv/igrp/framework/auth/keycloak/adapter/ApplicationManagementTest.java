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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Disabled
public class ApplicationManagementTest {

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
    private ClientsResource clientsResource;

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
    void shouldCreateApplicationSuccessfully() throws IAMException {
        String departmentName = "dept1";
        String applicationName = "app1";
        String departmentId = "dept-id-123";

        GroupRepresentation departmentGroup = new GroupRepresentation();
        departmentGroup.setId(departmentId);

        when(realmResource.groups()).thenReturn(groupsResource);
        when(realmResource.getGroupByPath(departmentName)).thenReturn(departmentGroup);
        when(groupsResource.group(departmentId)).thenReturn(groupResource);
        when(groupResource.subGroup(any(GroupRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);

        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.create(any(ClientRepresentation.class))).thenReturn(response);

        keycloakAdapter.createApplication(departmentName, applicationName);

        verify(groupResource).subGroup(any(GroupRepresentation.class));
        verify(clientsResource).create(any(ClientRepresentation.class));
        verify(response, times(2)).close();
    }

    @Test
    void shouldFailToCreateApplication() {
        String departmentName = "dept1";
        String applicationName = "app1";

        // Mock department group with an ID to avoid null
        GroupRepresentation departmentGroup = new GroupRepresentation();
        departmentGroup.setId("dept-id-123");

        when(realmResource.getGroupByPath(departmentName)).thenReturn(departmentGroup);
        when(realmResource.groups()).thenReturn(groupsResource); // <--- FIXED
        when(groupsResource.group("dept-id-123")).thenReturn(groupResource);
        when(groupResource.subGroup(any(GroupRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(500);

        IAMException thrown = assertThrows(IAMException.class, () ->
                keycloakAdapter.createApplication(departmentName, applicationName)
        );

        assertEquals("Error creating application", thrown.getMessage());
    }

    @Test
    void shouldUpdateApplicationSuccessfully() throws IAMException {
        String departmentName = "dept1";
        String oldName = "app1";
        String newName = "app1_new";
        String groupPath = departmentName + "/" + oldName;

        GroupRepresentation group = new GroupRepresentation();
        group.setId("999");
        group.setName(oldName);

        when(realmResource.getGroupByPath(groupPath)).thenReturn(group);
        when(realmResource.groups()).thenReturn(groupsResource);
        when(groupsResource.group("999")).thenReturn(groupResource);
        when(groupResource.toRepresentation()).thenReturn(group);

        keycloakAdapter.updateApplication(departmentName, oldName, newName);

        verify(groupResource).update(argThat(rep -> rep.getName().equals(newName)));
    }

    @Test
    void shouldThrowWhenUpdateApplicationFails() {
        String departmentName = "dept1";
        String oldName = "app1";
        String newName = "app1_new";
        String groupPath = departmentName + "/" + oldName;

        when(realmResource.getGroupByPath(groupPath)).thenThrow(new RuntimeException("Application not found"));

        IAMException ex = assertThrows(IAMException.class, () ->
                keycloakAdapter.updateApplication(departmentName, oldName, newName)
        );

        assertEquals("Error updating application", ex.getMessage());
    }

    @Test
    void shouldDeleteApplicationSuccessfully() throws IAMException {
        String departmentName = "dept1";
        String applicationName = "app1";
        String groupPath = departmentName + "/" + applicationName;

        GroupRepresentation group = new GroupRepresentation();
        group.setId("999");
        group.setName(applicationName);

        when(realmResource.getGroupByPath(groupPath)).thenReturn(group);
        when(realmResource.groups()).thenReturn(groupsResource);
        when(groupsResource.group("999")).thenReturn(groupResource);

        keycloakAdapter.deleteApplication(departmentName, applicationName);

        verify(groupResource).remove();
    }


}