package cv.igrp.framework.auth.core.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

/**
 * v3-21 (#29): records a business action with one call.
 *
 * <pre>{@code
 * @Transactional
 * public void approve(UUID id) {
 *     ...
 *     audit.record("fatura.aprovada", "faturas/" + id, Map.of("total", total));
 * }
 * }</pre>
 *
 * <p>The starter fills in the rest: the user ({@code sub} of the token, or {@code system}
 * outside a request), this service's name, the agent ({@code act.sub} of a token-exchange
 * token), the time, the result and the trace id. The event and its outbox row are written
 * in the caller's transaction (a new one if there is none), so a rolled-back action leaves
 * no event and a committed one always has its event.
 *
 * <p>The result is always {@value #SUCCESS}: an event only survives when the action commits.
 */
public class Audit {

    /** The only result of a recorded business action. */
    public static final String SUCCESS = "SUCCESS";
    /** The user recorded when there is no authenticated user, e.g. in a scheduled job. */
    public static final String SYSTEM_USER = "system";

    /** The event as stored, and as published from the outbox to Kafka. */
    record Event(UUID id, String event, String resource, Object payload, String username, String service,
                 String agent, String result, String traceId, Instant occurredAt) {
    }

    private static final String INSERT_EVENT = """
            INSERT INTO audit_event (id, event, resource, payload, username, service, agent, result, trace_id, occurred_at)
            VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?)""";
    private static final String INSERT_OUTBOX = """
            INSERT INTO audit_outbox (id, event_id, payload, created_at) VALUES (?, ?, ?::jsonb, ?)""";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final AuditSchema schema;
    private final String service;
    private final Clock clock;
    private final JsonMapper json = JsonMapper.builder().build();

    public Audit(JdbcTemplate jdbc, TransactionTemplate transactions, AuditSchema schema, String service, Clock clock) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.schema = schema;
        this.service = service;
        this.clock = clock;
    }

    /**
     * @param event    what happened, e.g. {@code fatura.aprovada}
     * @param resource what it happened to, e.g. {@code faturas/42}
     * @param payload  details, serialised as JSON; may be {@code null}
     * @throws IllegalStateException when the audit tables are not available (not PostgreSQL)
     */
    public void record(String event, String resource, Object payload) {
        String unavailable = schema.unavailableReason();
        if (unavailable != null) {
            throw new IllegalStateException(unavailable);
        }
        requireText(event, "event");
        requireText(resource, "resource");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Event recorded = new Event(UUID.randomUUID(), event, resource, payload, username(authentication), service,
                agent(authentication), SUCCESS, traceId(), clock.instant());
        OffsetDateTime at = OffsetDateTime.ofInstant(recorded.occurredAt(), ZoneOffset.UTC);
        String payloadJson = payload == null ? null : json.writeValueAsString(payload);
        String eventJson = json.writeValueAsString(recorded);

        transactions.executeWithoutResult(status -> {
            jdbc.update(INSERT_EVENT, recorded.id(), recorded.event(), recorded.resource(), payloadJson,
                    recorded.username(), recorded.service(), recorded.agent(), recorded.result(),
                    recorded.traceId(), at);
            jdbc.update(INSERT_OUTBOX, UUID.randomUUID(), recorded.id(), eventJson, at);
        });
    }

    private static String username(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM_USER;
        }
        if (authentication instanceof JwtAuthenticationToken token && token.getToken().getSubject() != null) {
            return token.getToken().getSubject();
        }
        return authentication.getName();
    }

    // RFC 8693: the current actor is the "sub" of the top-level "act" claim.
    private static String agent(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token
                && token.getToken().getClaims().get("act") instanceof Map<?, ?> act
                && act.get("sub") instanceof String actor) {
            return actor;
        }
        return null;
    }

    private static String traceId() {
        return CorrelationIds.current().orElseGet(() -> CorrelationIds.resolve(null));
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("audit.record needs a non-blank " + name);
        }
    }
}
