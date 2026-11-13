package cv.igrp.framework.auth.autentika.config;

import cv.igrp.framework.auth.autentika.adapter.AutentikaAdapter;
import cv.igrp.framework.auth.core.adapter.IAdapter;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AutentikaAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestApplication.class);

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication { }

    private ApplicationContextRunner configuredRunner() {
        return runner.withPropertyValues(
                "igrp.autentika.enabled=true",
                "igrp.autentika.registration-endpoint=https://example.test/register",
                "igrp.autentika.token-endpoint=https://example.test/token",
                "igrp.autentika.consumer-key=test-key",
                "igrp.autentika.consumer-secret=test-secret");
    }

    @Test
    void discoversAdapterAndBindsConfigurationWithoutNetworkCalls() {
        configuredRunner().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(IAdapter.class);
            assertThat(context.getBean(IAdapter.class)).isInstanceOf(AutentikaAdapter.class);
            var config = context.getBean(AutentikaConfiguration.class);
            assertThat(config.registrationEndpoint()).isEqualTo(URI.create("https://example.test/register"));
            assertThat(config.tokenEndpoint()).isEqualTo(URI.create("https://example.test/token"));
            assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(context.getBean(AutentikaSpringProperties.class).toString()).doesNotContain("test-secret");
        });
    }

    @Test
    void allowsCustomTimeout() {
        configuredRunner().withPropertyValues("igrp.autentika.request-timeout=5s").run(context ->
                assertThat(context.getBean(AutentikaConfiguration.class).requestTimeout())
                        .isEqualTo(Duration.ofSeconds(5)));
    }

    @Test
    void remainsDisabledUnlessExplicitlyEnabled() {
        runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(IAdapter.class));
        configuredRunner().withPropertyValues("igrp.autentika.enabled=false").run(context ->
                assertThat(context).hasNotFailed().doesNotHaveBean(IAdapter.class));
    }

    @Test
    void respectsUserProvidedAdapterWithoutRequiringCredentials() {
        var adapter = mock(IAdapter.class);
        runner.withPropertyValues("igrp.autentika.enabled=true")
                .withBean(IAdapter.class, () -> adapter).run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(IAdapter.class);
                    assertThat(context.getBean(IAdapter.class)).isSameAs(adapter);
                    assertThat(context).doesNotHaveBean(AutentikaConfiguration.class);
                });
    }

    @Test
    void failsAtStartupWhenEnabledWithoutRequiredConfiguration() {
        runner.withPropertyValues("igrp.autentika.enabled=true").run(context ->
                assertThat(context).hasFailed());
    }

    @Test
    void allowsCustomTokenSupplierThroughConfigurationBean() {
        var config = new AutentikaConfiguration(URI.create("https://example.test/register"),
                URI.create("https://example.test/token"), () -> "custom-token", Duration.ofSeconds(10));
        runner.withPropertyValues("igrp.autentika.enabled=true")
                .withBean(AutentikaConfiguration.class, () -> config).run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(IAdapter.class);
                    assertThat(context.getBean(AutentikaConfiguration.class)).isSameAs(config);
                });
    }
}
