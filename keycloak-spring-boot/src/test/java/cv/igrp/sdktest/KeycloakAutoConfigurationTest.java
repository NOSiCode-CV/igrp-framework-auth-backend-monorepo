package cv.igrp.sdktest;

import cv.igrp.framework.auth.core.security.IgrpJwtAuthenticationConverter;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Keycloak module registers itself through auto-configuration. With
 * core-spring-boot next to it, which of the two {@code JwtAuthenticationConverter}s
 * applied used to depend on registration order; now core-spring-boot's always wins.
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
    @DisplayName("With core-spring-boot present, its converter applies and Keycloak's backs off")
    void coreWins() {
        new ApplicationContextRunner()
                .withPropertyValues("igrp.access.api.base-url=http://localhost:1")
                .withConfiguration(AutoConfigurations.of(
                        KeycloakJwtAuthenticationConverter.class,
                        IgrpJwtAuthenticationConverter.class,
                        cv.igrp.framework.auth.core.autoconfig.AutoConfiguration.class))
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(JwtAuthenticationConverter.class).size());
                    Set<String> authorities = context.getBean(JwtAuthenticationConverter.class)
                            .convert(jwt("not-a-jwt")).getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());

                    assertTrue(authorities.contains("ROLE_ACTIVITI_USER"), authorities.toString());
                });
    }
}
