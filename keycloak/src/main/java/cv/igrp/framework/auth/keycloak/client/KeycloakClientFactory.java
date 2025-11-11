package cv.igrp.framework.auth.keycloak.client;

import cv.igrp.framework.auth.keycloak.config.KeycloakProperties;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

public class KeycloakClientFactory {

    private final KeycloakProperties properties;

    public KeycloakClientFactory(KeycloakProperties properties) {
        this.properties = properties;
    }

    public Keycloak createClient() {
        return KeycloakBuilder.builder()
                .serverUrl(properties.getServerUrl())
                .realm(properties.getRealm())
                .clientId(properties.getClientId())
                .clientSecret(properties.getClientSecret())
                .grantType(properties.getGrantType())
                .build();
    }

}
