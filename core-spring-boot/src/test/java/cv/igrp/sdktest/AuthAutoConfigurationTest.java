package cv.igrp.sdktest;

import cv.igrp.framework.auth.core.config.MethodSecurityEnableConfig;
import cv.igrp.framework.auth.core.config.MethodSecurityProperties;
import cv.igrp.framework.auth.core.security.AuthenticationHelper;
import cv.igrp.platform.access.client.ApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 0.3 starter's wiring on Boot 4: everything arrives through auto-configuration, an
 * application that still scans {@code cv.igrp} gets exactly the same, and the legacy
 * authorization beans are gone (v3-08).
 *
 * <p>Lives outside {@code cv.igrp.framework.auth} so the scan cannot pick up this test's
 * own fixtures.
 */
class AuthAutoConfigurationTest {

    private static final String BASE_URL = "igrp.access.api.base-url=http://localhost:1";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues(BASE_URL)
            .withConfiguration(AutoConfigurations.of(
                    cv.igrp.framework.auth.core.autoconfig.AutoConfiguration.class,
                    MethodSecurityEnableConfig.class));

    @Configuration(proxyBeanMethods = false)
    @ComponentScan("cv.igrp.framework.auth.core")
    static class LegacyScanningApplication {
    }

    private static Set<String> igrpBeanNames(ApplicationContext context) {
        return Arrays.stream(context.getBeanDefinitionNames())
                .filter(name -> {
                    Class<?> type = context.getType(name);
                    return type != null && type.getName().startsWith("cv.igrp.")
                            && !type.getName().startsWith("cv.igrp.sdktest")
                            && !name.contains(".")
                            && !type.getSimpleName().contains("AutoConfiguration")
                            && !type.getSimpleName().startsWith("MethodSecurity");
                })
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    @DisplayName("Every auto-configuration is registered in the imports file, and the legacy ones are gone")
    void importsFile() {
        List<String> imports = ImportCandidates
                .load(org.springframework.boot.autoconfigure.AutoConfiguration.class, getClass().getClassLoader())
                .getCandidates();

        assertTrue(imports.containsAll(List.of(
                "cv.igrp.framework.auth.core.autoconfig.AutoConfiguration",
                "cv.igrp.framework.auth.core.config.MethodSecurityEnableConfig",
                "cv.igrp.framework.auth.core.route.RouteSecurityAutoConfiguration")), imports.toString());
        assertFalse(imports.contains("cv.igrp.framework.auth.core.security.IgrpJwtAuthenticationConverter"), imports.toString());
        assertFalse(imports.contains("cv.igrp.framework.auth.core.config.MethodSecurityConfig"), imports.toString());
    }

    @Test
    @DisplayName("Without any scan the application gets the wiring, and no igrpAuthorization bean")
    void withoutScan() {
        runner.run(context -> {
            assertEquals(Set.of("authenticationHelper", "igrpApiClient"), igrpBeanNames(context));
            assertTrue(context.getBean("igrpApiClient") instanceof ApiClient);
            assertEquals(1, context.getBeansOfType(AuthenticationHelper.class).size());
            assertFalse(context.containsBean("igrpAuthorization"));
            assertTrue(context.containsBean("igrpPreAuthorize"));
        });
    }

    @Test
    @DisplayName("An application that still scans cv.igrp gets exactly the same beans, once each")
    void withLegacyScan() {
        runner.withUserConfiguration(LegacyScanningApplication.class).run(context -> {
            assertFalse(context.isClosed());
            assertNull(context.getStartupFailure());
            assertEquals(Set.of("authenticationHelper", "igrpApiClient"), igrpBeanNames(context));
        });
    }

    @Test
    @DisplayName("The AM client carries the service's client credentials when they are configured, with or without a scope")
    void apiClientCredentials() {
        runner.run(context -> assertNull(context.getBean(ApiClient.class).getClientCredentialsTokenProvider()));
        runner.withPropertyValues("igrp.access.m2m.client-id=svc", "igrp.access.m2m.client-secret=s")
                .run(context -> assertNotNull(context.getBean(ApiClient.class).getClientCredentialsTokenProvider()));
        runner.withPropertyValues("igrp.access.m2m.client-id=svc", "igrp.access.m2m.client-secret=s",
                        "igrp.access.m2m.scope=igrp.m2m")
                .run(context -> assertNotNull(context.getBean(ApiClient.class).getClientCredentialsTokenProvider()));
    }

    @Test
    @DisplayName("Method security can still be switched off by property")
    void methodSecurityProperty() {
        runner.withPropertyValues("igrp.framework.auth.method-security.enabled=false").run(context -> {
            assertTrue(context.getBeansOfType(MethodSecurityProperties.class).isEmpty());
            assertFalse(context.containsBean("igrpPreAuthorize"));
        });
    }
}
