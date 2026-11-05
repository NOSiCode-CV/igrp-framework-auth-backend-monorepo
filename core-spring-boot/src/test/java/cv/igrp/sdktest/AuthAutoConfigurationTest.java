package cv.igrp.sdktest;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import cv.igrp.framework.auth.core.config.MethodSecurityConfig;
import cv.igrp.framework.auth.core.config.MethodSecurityEnableConfig;
import cv.igrp.framework.auth.core.security.AuthenticationHelper;
import cv.igrp.framework.auth.core.security.IAuthorizationServiceAdapter;
import cv.igrp.framework.auth.core.security.IgrpAuthorizationService;
import cv.igrp.framework.auth.core.security.IgrpAuthorizationServiceAdapter;
import cv.igrp.framework.auth.core.security.IgrpJwtAuthenticationConverter;
import cv.igrp.platform.access.client.ApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The SDK's wiring on Boot 4: everything arrives through auto-configuration, an
 * application that still scans {@code cv.igrp} gets exactly the same, and the
 * authorities a JWT maps to are pinned.
 *
 * <p>Lives outside {@code cv.igrp.framework.auth} so the scan cannot pick up
 * this test's own fixtures.
 */
class AuthAutoConfigurationTest {

    private static final String BASE_URL = "igrp.access.api.base-url=http://localhost:1";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues(BASE_URL)
            .withConfiguration(AutoConfigurations.of(
                    cv.igrp.framework.auth.core.autoconfig.AutoConfiguration.class,
                    IgrpJwtAuthenticationConverter.class,
                    MethodSecurityConfig.class,
                    MethodSecurityEnableConfig.class));

    @Configuration(proxyBeanMethods = false)
    @ComponentScan("cv.igrp.framework.auth.core")
    static class LegacyScanningApplication {
    }

    private static Set<String> igrpBeanNames(org.springframework.context.ApplicationContext context) {
        return Arrays.stream(context.getBeanDefinitionNames())
                .filter(name -> {
                    Class<?> type = context.getType(name);
                    return type != null && type.getName().startsWith("cv.igrp.")
                            && !type.getName().startsWith("cv.igrp.sdktest")
                            && !name.contains(".")
                            && !type.getSimpleName().contains("AutoConfiguration")
                            && !type.getSimpleName().startsWith("MethodSecurity")
                            && type != IgrpJwtAuthenticationConverter.class;
                })
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    @DisplayName("Every auto-configuration is registered in the imports file")
    void importsFile() {
        List<String> imports = ImportCandidates
                .load(org.springframework.boot.autoconfigure.AutoConfiguration.class, getClass().getClassLoader())
                .getCandidates();

        assertTrue(imports.containsAll(List.of(
                "cv.igrp.framework.auth.core.autoconfig.AutoConfiguration",
                "cv.igrp.framework.auth.core.security.IgrpJwtAuthenticationConverter",
                "cv.igrp.framework.auth.core.config.MethodSecurityConfig",
                "cv.igrp.framework.auth.core.config.MethodSecurityEnableConfig")), imports.toString());
    }

    @Test
    @DisplayName("Without any scan the application gets the whole wiring, with the same bean names")
    void withoutScan() {
        runner.run(context -> {
            assertEquals(Set.of("authenticationHelper", "authorizationSyncRunner", "igrpApiClient",
                    "igrpAuthorization", "igrpAuthorizationServiceAdapter", "methodSecurityExpressionHandler"),
                    igrpBeanNames(context));
            assertTrue(context.getBean("igrpAuthorization") instanceof IgrpAuthorizationService);
            assertTrue(context.getBean("igrpApiClient") instanceof ApiClient);
            assertEquals(1, context.getBeansOfType(AuthenticationHelper.class).size());
            assertEquals(1, context.getBeansOfType(JwtAuthenticationConverter.class).size());
            assertEquals(1, context.getBeansOfType(MethodSecurityExpressionHandler.class).size());
        });
    }

    @Test
    @DisplayName("An application that still scans cv.igrp gets exactly the same beans, once each")
    void withLegacyScan() {
        runner.withUserConfiguration(LegacyScanningApplication.class).run(context -> {
            assertFalse(context.isClosed());
            assertEquals(null, context.getStartupFailure());
            assertEquals(Set.of("authenticationHelper", "authorizationSyncRunner", "igrpApiClient",
                    "igrpAuthorization", "igrpAuthorizationServiceAdapter", "methodSecurityExpressionHandler"),
                    igrpBeanNames(context));
            assertEquals(1, context.getBeansOfType(IAuthorizationServiceAdapter.class).size());
            assertEquals(1, context.getBeansOfType(JwtAuthenticationConverter.class).size());
        });
    }

    @Test
    @DisplayName("A custom adapter replaces the default one and still feeds the converter")
    void customAdapter() {
        runner.withBean(IAuthorizationServiceAdapter.class, AuthAutoConfigurationTest::rolesOnlyAdapter).run(context -> {
            assertEquals(1, context.getBeansOfType(IAuthorizationServiceAdapter.class).size());
            assertTrue(context.getBeansOfType(IgrpAuthorizationServiceAdapter.class).isEmpty());
            assertEquals(1, context.getBeansOfType(JwtAuthenticationConverter.class).size());
        });
    }

    @Test
    @DisplayName("With the default adapter switched off and none supplied, no converter is created")
    void adapterSwitchedOff() {
        runner.withPropertyValues("igrp.authorization.service.adapter=other").run(context -> {
            assertTrue(context.getBeansOfType(IAuthorizationServiceAdapter.class).isEmpty());
            assertTrue(context.getBeansOfType(JwtAuthenticationConverter.class).isEmpty());
        });
    }

    @Test
    @DisplayName("Method security can still be switched off by property")
    void methodSecurityProperty() {
        runner.withPropertyValues("igrp.framework.auth.method-security.enabled=false")
                .run(context -> assertTrue(context.getBeansOfType(
                        cv.igrp.framework.auth.core.config.MethodSecurityProperties.class).isEmpty()));
    }

    @Test
    @DisplayName("A JWT maps to the same authorities as before")
    void authorities() {
        runner.run(context -> {
            JwtAuthenticationConverter converter = context.getBean(JwtAuthenticationConverter.class);
            String token = new PlainJWT(new JWTClaimsSet.Builder()
                    .claim("selectedRole", "MANAGER")
                    .claim("org", "DEPT_A")
                    .claim("permissions", List.of("igrp.users.list", "igrp.users.view"))
                    .claim("is_super_admin", true)
                    .build()).serialize();
            Jwt jwt = Jwt.withTokenValue(token).header("alg", "none").subject("user-1")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();

            Set<String> authorities = converter.convert(jwt).getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toCollection(TreeSet::new));

            assertEquals(new TreeSet<>(Set.of(
                    "ROLE_MANAGER", "GROUP_MANAGER",
                    "igrp.users.list", "igrp.users.view",
                    "DEPT_A", "GROUP_DEPT_A",
                    IgrpAuthorizationService.SUPER_ADMIN_AUTHORITY,
                    "ROLE_ACTIVITI_USER",
                    // Added by Spring Security 7 to every bearer token, not by this SDK.
                    "FACTOR_BEARER")), authorities);
        });
    }

    private static IAuthorizationServiceAdapter rolesOnlyAdapter() {
        return new IAuthorizationServiceAdapter() {
            @Override
            public Set<String> getRoles(String jwt) {
                return Set.of("CUSTOM");
            }

            @Override
            public Set<String> getPermissions(String jwt) {
                return Set.of();
            }

            @Override
            public Set<String> getDepartments(String jwt) {
                return Set.of();
            }

            @Override
            public boolean isSuperAdmin(String jwt) {
                return false;
            }
        };
    }
}
