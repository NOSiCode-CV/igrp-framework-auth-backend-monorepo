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
import org.keycloak.admin.client.resource.ResourcePermissionResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static cv.igrp.framework.auth.keycloak.adapter.KeycloakAdapter.POLICY_SUFFIX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionManagementTest {

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
    private ClientsResource clientsResource;

    @Mock
    private ClientResource clientResource;

    @Mock
    private AuthorizationResource authorizationResource;

    @Mock
    private PoliciesResource policiesResource;

    @Mock
    private ResourcePermissionsResource resourcePermissionsResource;

    @Mock
    private PermissionsResource permissionsResource;

    @Mock
    private Response response;

    @BeforeEach
    void setUp() {
        when(keycloakClientFactory.createClient()).thenReturn(keycloak);
        when(keycloakProperties.getRealm()).thenReturn("igrp_iam");
        when(keycloakProperties.getClientId()).thenReturn("igrp");
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("igrp");
        clientRep.setId("client1");
        when(clientsResource.findByClientId("igrp")).thenReturn(List.of(clientRep));
        when(clientsResource.get("client1")).thenReturn(clientResource);
        when(clientResource.authorization()).thenReturn(authorizationResource);
        when(authorizationResource.policies()).thenReturn(policiesResource);
    }

    @Test
    void shouldCreatePermissionSuccessfully() throws IAMException {
        String permissionName = "perm1";
        String description = "Test Permission";
        String policyName = permissionName + POLICY_SUFFIX;

        when(authorizationResource.permissions()).thenReturn(permissionsResource);
        when(permissionsResource.resource()).thenReturn(resourcePermissionsResource);
        when(response.getStatus()).thenReturn(201);
        when(policiesResource.create(any(PolicyRepresentation.class))).thenReturn(response);
        when(resourcePermissionsResource.create(any(ResourcePermissionRepresentation.class))).thenReturn(response);

        keycloakAdapter.createPermission(permissionName, description);

        verify(policiesResource).create(argThat(policy ->
                policy.getName().equals(policyName) &&
                        policy.getDescription().equals(description)
        ));
        verify(resourcePermissionsResource).create(argThat(perm ->
                perm.getName().equals(permissionName) &&
                        perm.getPolicies().contains(policyName)
        ));
        verify(response, times(2)).getStatus();
    }

    @Test
    void shouldThrowExceptionWhenPolicyCreationFails() {
        String permissionName = "perm1";
        String description = "Test Permission";

        when(policiesResource.create(any(PolicyRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(500);

        IAMException exception = assertThrows(IAMException.class, () ->
                keycloakAdapter.createPermission(permissionName, description)
        );

        assertTrue(exception.getMessage().contains("Error creating permission"));
        verify(resourcePermissionsResource, never()).create(any());
    }

    @Test
    void shouldDeletePermissionSuccessfully() throws IAMException {
        String permissionName = "perm1";
        String policyName = permissionName + POLICY_SUFFIX;
        String permissionId = "perm1";
        String policyId = "policy1";

        ResourcePermissionRepresentation permRep = new ResourcePermissionRepresentation();
        permRep.setId(permissionId);
        ResourcePermissionResource permResource = mock(ResourcePermissionResource.class);

        PolicyRepresentation policyRep = new PolicyRepresentation();
        policyRep.setId(policyId);
        PolicyResource policyResource = mock(PolicyResource.class);

        when(authorizationResource.permissions()).thenReturn(permissionsResource);
        when(permissionsResource.resource()).thenReturn(resourcePermissionsResource);
        when(resourcePermissionsResource.findByName(permissionName)).thenReturn(permRep);
        when(resourcePermissionsResource.findById(permissionId)).thenReturn(permResource);

        when(policiesResource.findByName(policyName)).thenReturn(policyRep);
        when(policiesResource.policy(policyId)).thenReturn(policyResource);

        keycloakAdapter.deletePermission(permissionName);

        verify(permResource).remove();
        verify(policyResource).remove();
    }
}