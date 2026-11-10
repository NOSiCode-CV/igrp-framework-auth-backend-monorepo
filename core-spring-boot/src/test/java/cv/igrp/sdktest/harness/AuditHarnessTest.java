package cv.igrp.sdktest.harness;

import cv.igrp.framework.auth.core.audit.Audit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v3-21 (#29): the starter's business audit on PostgreSQL. The application connects as
 * {@code app_user}, an ordinary role (not a superuser, which would bypass every grant),
 * so the append-only grants the starter's migration sets are really enforced.
 */
class AuditHarnessTest extends AbstractStarterTest {

    @SuppressWarnings("resource")
    static final GenericContainer<?> POSTGRES =
            new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withEnv("POSTGRES_PASSWORD", "admin")
                    .withEnv("POSTGRES_DB", "app")
                    .withExposedPorts(5432)
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));

    static {
        POSTGRES.start();
        try {
            POSTGRES.execInContainer("psql", "-U", "postgres", "-d", "app", "-c",
                    "CREATE ROLE app_user LOGIN PASSWORD 'app'; GRANT USAGE, CREATE ON SCHEMA public TO app_user;");
        } catch (Exception e) {
            throw new IllegalStateException("Could not create the application role", e);
        }
    }

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/app");
        registry.add("spring.datasource.username", () -> "app_user");
        registry.add("spring.datasource.password", () -> "app");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Audit audit;

    @Autowired
    TransactionTemplate transactions;

    @Test
    @DisplayName("A committed action has one audit event and one outbox row, with every field filled by the starter")
    void committedAction() throws Exception {
        String resource = "faturas/" + UUID.randomUUID();
        mvc.perform(post("/audit/commit").param("resource", resource)
                        .header("X-Correlation-ID", "trace-abc")
                        .with(jwt().jwt(token -> token.subject("user-1")
                                .claim("act", Map.of("sub", "agent-x")))))
                .andExpect(status().isOk());

        List<Map<String, Object>> events = jdbc.queryForList("SELECT * FROM audit_event WHERE resource = ?", resource);
        assertEquals(1, events.size(), events.toString());
        Map<String, Object> event = events.getFirst();
        assertEquals("fatura.aprovada", event.get("event"));
        assertEquals("user-1", event.get("username"));
        assertEquals(SERVICE_NAME, event.get("service"));
        assertEquals("agent-x", event.get("agent"));
        assertEquals("SUCCESS", event.get("result"));
        assertEquals("trace-abc", event.get("trace_id"));
        assertNotNull(event.get("occurred_at"));
        assertTrue(event.get("payload").toString().contains("\"total\": 100"), event.toString());

        List<Map<String, Object>> outbox = jdbc.queryForList(
                "SELECT * FROM audit_outbox WHERE event_id = ?", event.get("id"));
        assertEquals(1, outbox.size(), outbox.toString());
        assertNull(outbox.getFirst().get("published_at"));
        assertTrue(outbox.getFirst().get("payload").toString().contains(resource), outbox.toString());
    }

    @Test
    @DisplayName("A rolled-back action leaves neither an audit event nor an outbox row")
    void rolledBackAction() throws Exception {
        String resource = "faturas/" + UUID.randomUUID();
        mvc.perform(post("/audit/rollback").param("resource", resource).with(jwt()))
                .andExpect(status().isConflict());

        assertEquals(0, count("SELECT count(*) FROM audit_event WHERE resource = ?", resource));
        assertEquals(0, count("SELECT count(*) FROM audit_outbox o JOIN audit_event e ON e.id = o.event_id "
                + "WHERE e.resource = ?", resource));
    }

    @Test
    @DisplayName("Outside a request the event is recorded as the system, with a generated trace id")
    void withoutUser() {
        String resource = "jobs/" + UUID.randomUUID();
        transactions.executeWithoutResult(status -> audit.record("job.run", resource, null));

        Map<String, Object> event = jdbc.queryForMap("SELECT * FROM audit_event WHERE resource = ?", resource);
        assertEquals("system", event.get("username"));
        assertNull(event.get("agent"));
        assertNotNull(event.get("trace_id"));
    }

    @Test
    @DisplayName("Recording outside a transaction fails and writes nothing")
    void withoutTransaction() {
        String resource = "faturas/" + UUID.randomUUID();
        Map<String, Object> payload = Map.of();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> audit.record("fatura.aprovada", resource, payload));
        assertTrue(failure.getMessage().contains("transaction"), failure.getMessage());
        assertEquals(0, count("SELECT count(*) FROM audit_event WHERE resource = ?", resource));
    }

    @Test
    @DisplayName("On a fresh database the service's own Flyway and the audit's each keep their own history")
    void besideTheServicesFlyway() {
        assertEquals(1, count("SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success"));
        assertEquals(1, count("SELECT count(*) FROM igrp_audit_schema_history WHERE version = '1' AND success"));
        assertEquals(0, count("SELECT count(*) FROM service_invoice"));
    }

    @Test
    @DisplayName("The application user cannot change or delete audit events")
    void auditEventsAreAppendOnly() {
        String resource = "faturas/" + UUID.randomUUID();
        transactions.executeWithoutResult(status -> audit.record("fatura.aprovada", resource, Map.of()));

        assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE audit_event SET result = 'FAILURE' WHERE resource = ?", resource));
        assertThrows(DataAccessException.class,
                () -> jdbc.update("DELETE FROM audit_event WHERE resource = ?", resource));
        assertThrows(DataAccessException.class, () -> jdbc.execute("TRUNCATE audit_event CASCADE"));
    }

    @Test
    @DisplayName("The application user can only mark outbox rows published")
    void outboxOnlyPublicationField() {
        String resource = "faturas/" + UUID.randomUUID();
        transactions.executeWithoutResult(status -> audit.record("fatura.aprovada", resource, Map.of()));
        String eventId = "(SELECT id FROM audit_event WHERE resource = '" + resource + "')";

        assertEquals(1, jdbc.update("UPDATE audit_outbox SET published_at = now() WHERE event_id = " + eventId));
        assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE audit_outbox SET payload = '{}' WHERE event_id = " + eventId));
        assertThrows(DataAccessException.class,
                () -> jdbc.update("DELETE FROM audit_outbox WHERE event_id = " + eventId));
    }

    private int count(String sql, Object... args) {
        Integer count = jdbc.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }
}
