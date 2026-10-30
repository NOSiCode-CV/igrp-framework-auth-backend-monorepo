package cv.igrp.framework.auth.keycloak.config;

import cv.igrp.framework.auth.core.adapter.IAdapter;
import cv.igrp.framework.auth.keycloak.adapter.KeycloakAdapter;
import cv.igrp.framework.auth.keycloak.client.KeycloakClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@org.springframework.boot.autoconfigure.AutoConfiguration
public class AutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "igrp.keycloak")
    @ConditionalOnMissingBean
    public KeycloakSpringProperties keycloakSpringProperties() {
        return new KeycloakSpringProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public KeycloakClientFactory keycloakClientFactory(KeycloakSpringProperties properties) {
        return new KeycloakClientFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public IAdapter iAdapter(KeycloakClientFactory factory, KeycloakSpringProperties properties) {
        return new KeycloakAdapter(factory, properties);
    }

}
