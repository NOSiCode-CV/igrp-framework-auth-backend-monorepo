package cv.igrp.framework.auth.keycloak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "igrp.keycloak")
@Configuration
public class KeycloakSpringProperties extends KeycloakProperties {
    // Inherits fields from ExternalSettings
}
