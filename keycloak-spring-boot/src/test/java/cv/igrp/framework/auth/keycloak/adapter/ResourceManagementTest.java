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
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled
public class ResourceManagementTest {

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
    private ResourcesResource resourcesResource;

    @Mock
    private Response response;

    @Mock
    private ResourceResource resourceResource;

    @BeforeEach
    void setUp() {
        final String realm = "igrp_iam";
        final String clientId = "igrp";
        final String clientUUID = "igrp";
        when(keycloakClientFactory.createClient()).thenReturn(keycloak);
        when(keycloakProperties.getRealm()).thenReturn(realm);
        when(keycloak.realm(realm)).thenReturn(realmResource);

        when(keycloakProperties.getClientId()).thenReturn(clientId);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.get(anyString())).thenReturn(clientResource);
        when(clientResource.authorization()).thenReturn(authorizationResource);
        when(authorizationResource.resources()).thenReturn(resourcesResource);

        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setClientId(clientId);
        clientRepresentation.setId(clientUUID);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId("igrp")).thenReturn(List.of(clientRepresentation));

    }

    @Test
    @Disabled
    void shouldCreateResourceSuccessfully() throws IAMException {
        // Given
        final String resourceName = "resource-1";
        final String description = "Test Resource";
        List<String> uris = List.of("/resource-1/*");
        List<String> scopes = List.of("view", "edit");
        final String urn = "urn:igrp:resource_test_1";

        ResourceRepresentation resource = new ResourceRepresentation();
        resource.setName(resourceName);
        resource.setDisplayName(description);
        resource.setUris(new HashSet<>(uris));
        resource.setType(urn);
        resource.setOwnerManagedAccess(false);

        when(resourcesResource.create(resource)).thenReturn(response);
        when(response.getStatus()).thenReturn(201);

        // When
        keycloakAdapter.createResource(resourceName, description, uris, scopes);

        // Then
        verify(resourcesResource).create(resource);
    }

    @Test
    @Disabled
    void shouldFailToCreateResource() {
        final String resourceName = "resource_1";
        final String description = "Desc...";
        final List<String> uris = List.of("/resource-1/*");

        when(resourcesResource.create(any())).thenReturn(response);
        when(response.getStatus()).thenReturn(500);

        IAMException ex = assertThrows(IAMException.class, () ->
                keycloakAdapter.createResource(resourceName, description, uris, null));

        assertEquals("Error creating resource", ex.getMessage());
    }

    @Test
    @Disabled
    void testCreateResource_exceptionThrown() {
        when(resourcesResource.create(any())).thenThrow(new RuntimeException("Error..."));
        IAMException ex = assertThrows(IAMException.class, () ->
                keycloakAdapter.createResource("resource_1", "Description...", List.of("/resource-1"), null));
        assertEquals("Error creating resource", ex.getMessage());
    }

    @Test
    @Disabled
    void shouldDeleteResourceSuccessfully() throws IAMException {
        // Given
        String resourceName = "resource_1";
        String resourceId = "999";

        ResourceRepresentation resource = new ResourceRepresentation();
        resource.setId(resourceId);
        resource.setName(resourceName);
        List<ResourceRepresentation> resources = List.of(resource);

        when(resourcesResource.findByName(resourceName)).thenReturn(resources);
        when(resourcesResource.resource(resourceId)).thenReturn(resourceResource);

        // When
        keycloakAdapter.deleteResource(resourceName);

        // Then
        verify(resourcesResource).findByName(resourceName);
        verify(resourceResource).remove();
    }

    @Test
    @Disabled
    void shouldThrowExceptionWhenResourceNotFound() {
        // Given
        String resourceName = "resource_1";
        when(resourcesResource.findByName(resourceName)).thenReturn(Collections.emptyList());

        // When / Then
        IAMException ex = assertThrows(IAMException.class, () -> keycloakAdapter.deleteResource(resourceName));

        verify(resourcesResource).findByName(resourceName);
        assertEquals("Error deleting resource", ex.getMessage());
    }

}
