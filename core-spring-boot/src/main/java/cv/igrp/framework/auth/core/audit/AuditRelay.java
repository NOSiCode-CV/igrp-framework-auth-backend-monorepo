package cv.igrp.framework.auth.core.audit;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.InvalidRecordException;
import org.apache.kafka.common.errors.RecordBatchTooLargeException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * v3-22 (#32): publishes the service's unpublished outbox rows to Kafka (topic
 * {@code igrp.audit}) and marks each one published only after the broker
 * acknowledges it. A broker outage only delays consolidation: the rows stay
 * unpublished and go out on a later round.
 *
 * <p>One replica relays at a time (a transaction-scoped advisory lock), so the
 * rows go out in the order they were claimed, keyed by the service: one
 * partition per service keeps that order. A round stops at the first failure;
 * the rest wait for the next round. A row whose acknowledgement arrived but
 * whose transaction then failed is published again: AM drops the duplicate by
 * the event's id.
 *
 * <p>A row Kafka can never take (larger than the broker accepts, say) is set
 * aside instead, with {@code failed_at} and the reason, and an error log, so it
 * does not hold back the rows after it. Clearing {@code failed_at} sends it again.
 */
public class AuditRelay {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditRelay.class);

    /**
     * The advisory lock of the relay, distinct from the service's own locks. Advisory locks are
     * database-wide, so the key names the schema whose outbox is relayed: services sharing one
     * database in their own schemas do not wait for each other.
     */
    static final String ONE_RELAY =
            "SELECT pg_try_advisory_xact_lock(hashtext('igrp.audit.relay:' || current_schema()))";

    private static final String CLAIM = """
            SELECT id, payload::text AS payload FROM audit_outbox
             WHERE published_at IS NULL AND failed_at IS NULL
             ORDER BY created_at, id
             LIMIT ?
               FOR UPDATE""";

    private static final String MARK_PUBLISHED = "UPDATE audit_outbox SET published_at = now() WHERE id = ?";

    private static final String SET_ASIDE = "UPDATE audit_outbox SET failed_at = now(), failure = ? WHERE id = ?";

    private static final int MAX_FAILURE = 1000;

    /** Failures of the record itself: sending it again cannot succeed. */
    private static final List<Class<? extends Throwable>> RECORD_FAILURES = List.of(RecordTooLargeException.class,
            RecordBatchTooLargeException.class, InvalidRecordException.class, SerializationException.class);

    /** How one send ended. */
    enum Outcome { ACKNOWLEDGED, RETRY_LATER, NEVER }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final Producer<String, String> producer;
    private final String topic;
    private final String service;
    private final int batch;
    private final Duration sendTimeout;

    public AuditRelay(JdbcTemplate jdbc, TransactionTemplate transactions, Producer<String, String> producer,
                      String topic, String service, int batch, Duration sendTimeout) {
        if (service == null || service.isBlank()) {
            // AM refuses an event without its service: it would be marked published and lost.
            throw new IllegalStateException("[AUDIT] The outbox relay needs the service's name: set "
                    + "igrp.security.service-name or spring.application.name");
        }
        if (batch < 1) {
            // drain() rounds while a batch is full: with 0 it would never stop.
            throw new IllegalArgumentException("[AUDIT] igrp.audit.relay.batch must be at least 1, was " + batch);
        }
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.producer = producer;
        this.topic = topic;
        this.service = service;
        this.batch = batch;
        this.sendTimeout = sendTimeout;
    }

    /**
     * One round: publishes up to one batch, unless another replica is relaying.
     * @return how many rows were handled: acknowledged and marked, or set aside
     */
    public int relayOnce() {
        Integer handled = transactions.execute(status -> {
            if (!Boolean.TRUE.equals(jdbc.queryForObject(ONE_RELAY, Boolean.class))) {
                return 0;
            }
            List<Map<String, Object>> rows = jdbc.queryForList(CLAIM, batch);
            int done = 0;
            for (Map<String, Object> row : rows) {
                Outcome outcome = send(row);
                if (outcome == Outcome.RETRY_LATER) {
                    break;
                }
                if (outcome == Outcome.ACKNOWLEDGED) {
                    jdbc.update(MARK_PUBLISHED, row.get("id"));
                }
                done++;
            }
            return done;
        });
        return Objects.requireNonNullElse(handled, 0);
    }

    /** Rounds until a batch is not full, so a backlog drains at once. @return rows handled */
    public int drain() {
        int total = 0;
        int round;
        do {
            round = relayOnce();
            total += round;
        } while (round == batch);
        return total;
    }

    private Outcome send(Map<String, Object> row) {
        try {
            producer.send(new ProducerRecord<>(topic, service, (String) row.get("payload")))
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return Outcome.ACKNOWLEDGED;
        } catch (ExecutionException ex) {
            return failed(row, ex.getCause() == null ? ex : ex.getCause());
        } catch (TimeoutException ex) {
            LOGGER.warn("[AUDIT] No acknowledgement from {} in {} (broker unavailable?); the outbox keeps the rest",
                    topic, sendTimeout);
            return Outcome.RETRY_LATER;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Outcome.RETRY_LATER;
        } catch (RuntimeException ex) {
            // send() itself throws when metadata is unavailable past max.block.ms, or the record cannot be serialised.
            return failed(row, ex);
        }
    }

    private Outcome failed(Map<String, Object> row, Throwable failure) {
        if (RECORD_FAILURES.stream().noneMatch(type -> type.isInstance(failure))) {
            LOGGER.warn("[AUDIT] Could not publish to {} (broker unavailable?); the outbox keeps the rest: {}",
                    topic, failure.getMessage());
            return Outcome.RETRY_LATER;
        }
        String reason = (failure.getClass().getSimpleName() + ": " + failure.getMessage());
        reason = reason.length() > MAX_FAILURE ? reason.substring(0, MAX_FAILURE) : reason;
        LOGGER.error("[AUDIT] Outbox row {} can never be published to {}; set aside (clear failed_at to retry): {}",
                row.get("id"), topic, reason);
        jdbc.update(SET_ASIDE, reason, row.get("id"));
        return Outcome.NEVER;
    }
}
