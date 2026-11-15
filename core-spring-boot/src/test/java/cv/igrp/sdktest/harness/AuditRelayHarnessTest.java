package cv.igrp.sdktest.harness;

import cv.igrp.framework.auth.core.audit.Audit;
import cv.igrp.framework.auth.core.audit.AuditRelay;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3-22 (#32): the outbox relay against Kafka and PostgreSQL. The application connects as
 * {@code app_user}, which may only set {@code published_at} on the outbox, so the relay is
 * tested with the grants it has in production. Pausing the broker's container is an outage.
 */
class AuditRelayHarnessTest extends AbstractStarterTest {

    private static final String TOPIC = "igrp.audit";

    @SuppressWarnings("resource")
    static final GenericContainer<?> POSTGRES =
            new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withEnv("POSTGRES_PASSWORD", "admin")
                    .withEnv("POSTGRES_DB", "app")
                    .withExposedPorts(5432)
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));

    static final KafkaBroker KAFKA = new KafkaBroker();

    static {
        POSTGRES.start();
        KAFKA.start();
        try {
            POSTGRES.execInContainer("psql", "-U", "postgres", "-d", "app", "-c",
                    "CREATE ROLE app_user LOGIN PASSWORD 'app'; GRANT USAGE, CREATE ON SCHEMA public TO app_user;");
        } catch (Exception e) {
            throw new IllegalStateException("Could not create the application role", e);
        }
    }

    @DynamicPropertySource
    static void postgresAndKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/app");
        registry.add("spring.datasource.username", () -> "app_user");
        registry.add("spring.datasource.password", () -> "app");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("igrp.audit.relay.bootstrap-servers", KAFKA::bootstrapServers);
        // The test drives the rounds itself.
        registry.add("igrp.audit.relay.interval", () -> "1h");
        registry.add("igrp.audit.relay.send-timeout", () -> "3s");
    }

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    Audit audit;
    @Autowired
    AuditRelay relay;
    @Autowired
    TransactionTemplate transactions;

    @Test
    @DisplayName("Outbox rows are published to igrp.audit and marked published only after the ack")
    void publishedAfterAck() {
        String resource = recordAction();

        relay.drain();

        assertTrue(published(resource), "marked published");
        List<ConsumerRecord<String, String>> received = consume();
        assertTrue(received.stream().anyMatch(r -> r.value().contains(resource) && SERVICE_NAME.equals(r.key())),
                "the event is on the topic, keyed by service");
    }

    @Test
    @DisplayName("With the broker down, rows stay unpublished; they go out when it returns")
    void brokerOutageOnlyDelays() {
        relay.drain();
        KAFKA.pause();
        String resource;
        try {
            resource = recordAction();
            relay.drain();
            assertTrue(!published(resource), "nothing marked while the broker is down");
        } finally {
            KAFKA.unpause();
        }

        long until = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (!published(resource) && System.nanoTime() < until) {
            relay.drain();
        }
        assertTrue(published(resource), "published once the broker is back");
        assertTrue(consume().stream().anyMatch(r -> r.value().contains(resource)));
    }

    @Test
    @DisplayName("A row larger than the broker accepts is set aside with its reason; the next row still goes out")
    void tooLargeIsSetAside() {
        relay.drain();
        String tooLarge = recordAction(Map.of("blob", "x".repeat(1_200_000)));
        String next = recordAction();

        relay.drain();

        assertTrue(!published(tooLarge), "the large row is not marked published");
        assertEquals("RecordTooLargeException", jdbc.queryForObject("""
                SELECT split_part(o.failure, ':', 1) FROM audit_outbox o JOIN audit_event e ON e.id = o.event_id
                 WHERE e.resource = ? AND o.failed_at IS NOT NULL""", String.class, tooLarge));
        assertTrue(published(next), "the row after it is published");
    }

    /** One committed business action. @return its resource */
    private String recordAction() {
        return recordAction(Map.of("total", 1));
    }

    private String recordAction(Map<String, Object> payload) {
        String resource = "faturas/" + UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user-1", null, "ROLE"));
        try {
            transactions.executeWithoutResult(status -> audit.record("fatura.aprovada", resource, payload));
        } finally {
            SecurityContextHolder.clearContext();
        }
        return resource;
    }

    private boolean published(String resource) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT bool_and(o.published_at IS NOT NULL) FROM audit_outbox o
                  JOIN audit_event e ON e.id = o.event_id WHERE e.resource = ?""", Boolean.class, resource));
    }

    private static List<ConsumerRecord<String, String>> consume() {
        List<ConsumerRecord<String, String>> all = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.bootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(TOPIC));
            long until = System.nanoTime() + Duration.ofSeconds(20).toNanos();
            while (System.nanoTime() < until) {
                consumer.poll(Duration.ofMillis(500)).forEach(all::add);
                if (!all.isEmpty() && consumer.assignment().stream()
                        .allMatch(p -> consumer.position(p) >= consumer.endOffsets(List.of(p)).get(p))) {
                    break;
                }
            }
        }
        assertEquals(false, all.isEmpty(), "records on " + TOPIC);
        return all;
    }
}
