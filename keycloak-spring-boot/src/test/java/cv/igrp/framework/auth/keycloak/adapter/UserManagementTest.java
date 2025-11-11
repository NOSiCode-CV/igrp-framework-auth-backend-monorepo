package cv.igrp.framework.auth.keycloak.adapter;

import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.core.model.IGRPUserRepresentation;
import cv.igrp.framework.auth.core.model.UserIdentity;
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
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserManagementTest {

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
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private GroupsResource groupsResource;

    @Mock
    private GroupResource groupResource;

    @Mock
    private Response response;

    @Mock
    private ClientsResource clientsResource;

    @Mock
    private ClientResource clientResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private RoleResource roleResource;

    @BeforeEach
    void setUp() {
        when(keycloakClientFactory.createClient()).thenReturn(keycloak);
        when(keycloakProperties.getRealm()).thenReturn("igrp_iam");
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
    }

    @Test
    void shouldCreateUserSuccessfully() throws IAMException {
        UserIdentity userIdentity = IGRPUserRepresentation.builder()
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .emailVerified(true)
                .enable(true)
                .build();

        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);

        keycloakAdapter.createUser(userIdentity);

        ArgumentCaptor<UserRepresentation> userCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(usersResource).create(userCaptor.capture());
        verify(response).close();

        UserRepresentation capturedUser = userCaptor.getValue();
        assertEquals("testuser", capturedUser.getUsername());
        assertEquals("test@example.com", capturedUser.getEmail());
        assertTrue(capturedUser.isEmailVerified());
        assertTrue(capturedUser.isEnabled());
    }

    @Test
    void shouldFailToCreateUser() {
        UserIdentity userIdentity = IGRPUserRepresentation.builder()
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(400);

        IAMException exception = assertThrows(IAMException.class, () ->
                keycloakAdapter.createUser(userIdentity)
        );

        assertEquals("Error creating user", exception.getMessage());
        verify(response).close();
    }

    @Test
    void shouldResolveUserSuccessfully() {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setId("999");
        userRep.setUsername("demo");
        userRep.setEmail("demo@example.com");
        userRep.setFirstName("John");
        userRep.setLastName("Doe");

        when(usersResource.search(eq("demo"), eq(true))).thenReturn(List.of(userRep));

        Optional<UserIdentity> result = keycloakAdapter.resolveUser("demo");

        assertTrue(result.isPresent());
        assertEquals("999", result.get().getId());
        System.out.println("USER: \n\n"
                + result.get().getEmail() +
                "\n"
                + result.get().getFirstName() +
                "\n"
                + result.get().getLastName() +
                "\n"
                + result.get().getUsername() +
                "\n"
                + result.get().getId() +
                "\n\n");
        //assertEquals("demo", result.get().getUsername());
        assertEquals("demo@example.com", result.get().getEmail());
    }

    @Test
    void shouldAssignRoleToUserSuccessfully() throws IAMException {
        String departmentName = "dept1";
        String roleName = "role1";
        String userId = "user1";
        String groupId = "group1";

        UserIdentity userIdentity = IGRPUserRepresentation.builder()
                .id(userId)
                .username("testuser")
                .build();

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setId(groupId);
        groupRep.setName(roleName);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("igrp");
        clientRep.setId("client1");

        when(keycloakProperties.getClientId()).thenReturn("igrp");
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("igrp")).thenReturn(List.of(clientRep));

        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);

        String roleGroupPath = departmentName + "/" + roleName;
        when(realmResource.getGroupByPath(roleGroupPath)).thenReturn(groupRep);

        KeycloakAdapter spyAdapter = spy(keycloakAdapter);
        doReturn(Optional.of(userIdentity)).when(spyAdapter).resolveUser("testuser");

        spyAdapter.assignRoleToUser(departmentName, roleName, "testuser");

        verify(userResource).joinGroup(groupId);
    }

    @Test
    void shouldUnassignRoleFromUserSuccessfully() throws IAMException {
        String departmentName = "dept1";
        String roleName = "role1";
        String userId = "user1";
        String groupId = "group1";
        String roleGroupPath = departmentName + "/" + roleName;

        UserIdentity userIdentity = IGRPUserRepresentation.builder()
                .id(userId)
                .username("testuser")
                .build();

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setId(groupId);
        groupRep.setName(roleName);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("igrp");
        clientRep.setId("client1");

        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);

        when(realmResource.getGroupByPath(roleGroupPath)).thenReturn(groupRep);

        KeycloakAdapter spyAdapter = spy(keycloakAdapter);
        doReturn(Optional.of(userIdentity)).when(spyAdapter).resolveUser("testuser");

        spyAdapter.unassignRoleFromUser(departmentName, roleName, "testuser");

        verify(userResource).leaveGroup(groupId);
    }
}