package cv.igrp.framework.auth.keycloak.config;

import cv.igrp.framework.auth.core.adapter.IAdapter;
import cv.igrp.framework.auth.keycloak.adapter.KeycloakAdapter;
import cv.igrp.framework.auth.keycloak.client.KeycloakClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoConfiguration {
    
    @Bean
    public KeycloakClientFactory keycloakClientFactory(KeycloakSpringProperties properties) {
        return new KeycloakClientFactory(properties);
    }

    @Bean
    public IAdapter iAdapter(KeycloakClientFactory factory, KeycloakSpringProperties properties) {
        return new KeycloakAdapter(factory, properties);
    }

}
