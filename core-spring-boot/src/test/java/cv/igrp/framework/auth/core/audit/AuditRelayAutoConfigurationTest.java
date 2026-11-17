package cv.igrp.framework.auth.core.audit;

import cv.igrp.framework.auth.core.audit.AuditRelayAutoConfiguration.AuditRelaySchedule;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** v3-22 (#32): when the starter starts an outbox relay, and when it refuses to. */
class AuditRelayAutoConfigurationTest {

    /** Converts "5s" into a Duration, as a Boot application does. */
    private static ApplicationContextRunner boot() {
        return new ApplicationContextRunner().withInitializer(context -> context.getBeanFactory()
                .setConversionService(ApplicationConversionService.getSharedInstance()));
    }

    private final ApplicationContextRunner runner = boot()
            .withConfiguration(AutoConfigurations.of(AuditRelayAutoConfiguration.class))
            .withUserConfiguration(Service.class)
            .withPropertyValues("igrp.security.service-name=faturacao-api", "igrp.audit.relay.interval=1h");

    /** A service with the business audit on, and a stand-in producer instead of a real broker. */
    @Configuration(proxyBeanMethods = false)
    static class Service {
        @Bean
        Audit audit() {
            return mock(Audit.class);
        }

        @Bean
        DataSource dataSource() {
            return mock(DataSource.class);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        Producer<String, String> igrpAuditProducer() {
            return new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
        }
    }

    @Test
    @DisplayName("With the broker's address, the relay runs from startup to shutdown")
    void relayRuns() {
        runner.withPropertyValues("igrp.audit.relay.bootstrap-servers=kafka:9092").run(context -> {
            assertThat(context).hasSingleBean(AuditRelay.class).hasSingleBean(AuditRelaySchedule.class);
            assertThat(context.getBean(AuditRelaySchedule.class).isRunning()).isTrue();
        });
    }

    @Test
    @DisplayName("Without the broker's address there is no relay: rows stay in the outbox, as before")
    void offWithoutTheBroker() {
        runner.run(context -> assertThat(context).doesNotHaveBean(AuditRelay.class)
                .doesNotHaveBean(AuditRelaySchedule.class));
    }

    @Test
    @DisplayName("Without kafka-clients on the classpath there is no relay")
    void offWithoutKafkaClients() {
        runner.withPropertyValues("igrp.audit.relay.bootstrap-servers=kafka:9092")
                .withClassLoader(new FilteredClassLoader(KafkaProducer.class))
                .run(context -> assertThat(context).doesNotHaveBean(AuditRelay.class));
    }

    @Test
    @DisplayName("Without the business audit there is nothing to relay")
    void offWithoutTheAudit() {
        boot()
                .withConfiguration(AutoConfigurations.of(AuditRelayAutoConfiguration.class))
                .withPropertyValues("igrp.audit.relay.bootstrap-servers=kafka:9092",
                        "igrp.security.service-name=faturacao-api")
                .run(context -> assertThat(context).doesNotHaveBean(AuditRelay.class));
    }

    @Test
    @DisplayName("A relay without the service's name refuses to start: AM would refuse its events")
    void refusesWithoutTheServiceName() {
        boot()
                .withConfiguration(AutoConfigurations.of(AuditRelayAutoConfiguration.class))
                .withUserConfiguration(Service.class)
                .withPropertyValues("igrp.audit.relay.bootstrap-servers=kafka:9092")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasStackTraceContaining("igrp.security.service-name"));
    }

    @Test
    @DisplayName("A batch below 1 is refused: a backlog would never stop draining")
    void refusesAnEmptyBatch() {
        runner.withPropertyValues("igrp.audit.relay.bootstrap-servers=kafka:9092", "igrp.audit.relay.batch=0")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasStackTraceContaining("igrp.audit.relay.batch"));
    }

    @Test
    @DisplayName("A service's own schedule replaces the starter's")
    void ownSchedule() {
        AuditRelaySchedule own = new AuditRelaySchedule(mock(AuditRelay.class), java.time.Duration.ofHours(1));
        runner.withPropertyValues("igrp.audit.relay.bootstrap-servers=kafka:9092")
                .withBean("ownSchedule", AuditRelaySchedule.class, () -> own)
                .run(context -> assertThat(context.getBean(AuditRelaySchedule.class)).isSameAs(own));
    }
}
