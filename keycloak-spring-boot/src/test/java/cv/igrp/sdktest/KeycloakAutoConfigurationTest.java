package cv.igrp.sdktest;

import cv.igrp.framework.auth.keycloak.security.KeycloakJwtAuthenticationConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Keycloak module registers itself through auto-configuration. Its JWT converter
 * maps Keycloak roles for services without core-spring-boot; the 0.3 starter's route
 * security sets its own converter, so the two never compete.
 */
class KeycloakAutoConfigurationTest {

    private static Jwt jwt(String token) {
        return Jwt.withTokenValue(token).header("alg", "none").subject("user-1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .claim("realm_access", Map.of("roles", List.of("realm-role")))
                .claim("resource_access", Map.of("app", Map.of("roles", List.of("client-role"))))
                .build();
    }

    @Test
    @DisplayName("Both auto-configurations are registered in the imports file")
    void importsFile() {
        List<String> imports = ImportCandidates
                .load(org.springframework.boot.autoconfigure.AutoConfiguration.class, getClass().getClassLoader())
                .getCandidates();

        assertTrue(imports.containsAll(List.of(
                "cv.igrp.framework.auth.keycloak.config.AutoConfiguration",
                "cv.igrp.framework.auth.keycloak.security.KeycloakJwtAuthenticationConverter")), imports.toString());
    }

    @Test
    @DisplayName("On its own, the Keycloak converter maps realm and client roles")
    void keycloakAlone() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KeycloakJwtAuthenticationConverter.class))
                .run(context -> {
                    Set<String> authorities = context.getBean(JwtAuthenticationConverter.class)
                            .convert(jwt("t")).getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());

                    // FACTOR_BEARER is added by Spring Security 7 to every bearer token.
                    assertEquals(Set.of("realm-role", "client-role", "FACTOR_BEARER"), authorities);
                });
    }

    @Test
    @DisplayName("Next to the 0.3 starter it still loads alone: the starter has no JWT converter bean of its own")
    void nextToTheStarter() {
        new ApplicationContextRunner()
                .withPropertyValues("igrp.access.api.base-url=http://localhost:1")
                .withConfiguration(AutoConfigurations.of(
                        KeycloakJwtAuthenticationConverter.class,
                        cv.igrp.framework.auth.core.autoconfig.AutoConfiguration.class))
                .run(context -> {
                    // The starter's route security sets its own converter on the chain (v3-08),
                    // so this bean is only used by services without the starter.
                    assertEquals(1, context.getBeansOfType(JwtAuthenticationConverter.class).size());
                    assertFalse(context.containsBean("igrpAuthorization"));
                });
    }
}
