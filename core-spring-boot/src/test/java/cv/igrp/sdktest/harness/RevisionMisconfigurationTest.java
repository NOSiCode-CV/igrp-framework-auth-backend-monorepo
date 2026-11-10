package cv.igrp.sdktest.harness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * v3-24 (#31): a service whose schema or entity scan does not let the starter's
 * revision work fails at startup with the fix in the message, instead of
 * silently losing username and trace id (or failing on the first write).
 */
class RevisionMisconfigurationTest {

    private static final StubAccessManagement AM = StubAccessManagement.start();

    @Test
    @DisplayName("revinfo without username/trace_id stops startup and names the columns to add")
    void missingColumnsFailStartup() {
        assertThatThrownBy(() -> start(StarterTestApplication.class,
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:revision/revinfo-without-columns.sql"))
                .hasStackTraceContaining("The revinfo table lacks the iGRP revision columns")
                .hasStackTraceContaining("ADD COLUMN username")
                .hasStackTraceContaining("ADD COLUMN trace_id");
    }

    @Test
    @DisplayName("An explicit @EntityScan that leaves out the revision entity stops startup")
    void entityScanWithoutTheRevisionFailsStartup() {
        assertThatThrownBy(() -> start(new Class<?>[]{StarterTestApplication.class, ExplicitEntityScan.class},
                "spring.jpa.hibernate.ddl-auto=create-drop"))
                .hasStackTraceContaining("Add its package to your @EntityScan")
                .hasStackTraceContaining("cv.igrp.framework.auth.core.audit.revision");
    }

    @Test
    @DisplayName("The revision can be switched off")
    void canBeDisabled() {
        try (ConfigurableApplicationContext context = start(StarterTestApplication.class,
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:revision/revinfo-without-columns.sql",
                "igrp.audit.revision.enabled=false")) {
            assertThat(context.isRunning()).isTrue();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = Note.class)
    static class ExplicitEntityScan {
    }

    private static ConfigurableApplicationContext start(Class<?> source, String... properties) {
        return start(new Class<?>[]{source}, properties);
    }

    private static ConfigurableApplicationContext start(Class<?>[] sources, String... properties) {
        return new SpringApplicationBuilder(sources)
                .properties("spring.main.web-application-type=none",
                        "spring.application.name=revision-misconfiguration",
                        "spring.datasource.url=jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                        "igrp.access.api.base-url=" + AM.baseUrl(),
                        "igrp.access.m2m.client-id=x",
                        "igrp.access.m2m.client-secret=y",
                        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=" + AM.baseUrl() + "/oauth2/jwks")
                .properties(properties)
                .run();
    }
}
