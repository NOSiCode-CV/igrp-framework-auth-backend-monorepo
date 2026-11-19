package cv.igrp.framework.auth.core.audit;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v3-22 (#32): the relay marks a row published only after the broker acknowledges it,
 * stops at the first failure, and sets aside a row Kafka can never take. Against Kafka itself in {@code AuditRelayHarnessTest}.
 */
class AuditRelayTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private final List<Map<String, Object>> rows = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void transactionsInline() {
        when(transactions.execute(any())).thenAnswer(call ->
                call.getArgument(0, TransactionCallback.class).doInTransaction(mock(TransactionStatus.class)));
        when(jdbc.queryForList(anyString(), anyInt())).thenAnswer(call -> List.copyOf(rows));
        when(jdbc.queryForObject(contains("advisory"), eq(Boolean.class))).thenReturn(true);
        for (int i = 1; i <= 3; i++) {
            rows.add(Map.of("id", "row-" + i, "payload", "{\"n\":" + i + "}"));
        }
    }

    @Test
    @DisplayName("Every acknowledged row is published to igrp.audit, keyed by service, in order, and marked")
    void acknowledgedRowsAreMarked() {
        MockProducer<String, String> producer = producer(true);

        int published = relay(producer).relayOnce();

        assertEquals(3, published);
        assertEquals(List.of("{\"n\":1}", "{\"n\":2}", "{\"n\":3}"),
                producer.history().stream().map(ProducerRecord::value).toList());
        producer.history().forEach(sent -> {
            assertEquals("igrp.audit", sent.topic());
            assertEquals("faturacao-api", sent.key());
        });
        verify(jdbc).update(anyString(), eq("row-1"));
        verify(jdbc).update(anyString(), eq("row-3"));
    }

    @Test
    @DisplayName("A failed send leaves that row and the rest unpublished; the next round sends them")
    void failureStopsTheRound() {
        MockProducer<String, String> producer = producer(false);
        AuditRelay relay = relay(producer);
        Thread broker = new Thread(() -> {
            awaitSends(producer, 1);
            producer.completeNext();
            awaitSends(producer, 2);
            producer.errorNext(new IllegalStateException("broker down"));
        });
        broker.start();

        int published = relay.relayOnce();

        assertEquals(1, published);
        verify(jdbc, times(1)).update(anyString(), any(Object.class));
        verify(jdbc, never()).update(anyString(), eq("row-3"));
    }

    @Test
    @DisplayName("A send that cannot even start (no metadata) marks nothing")
    void sendThatThrowsMarksNothing() {
        MockProducer<String, String> producer = producer(true);
        producer.sendException = new IllegalStateException("metadata not available");

        assertEquals(0, relay(producer).relayOnce());
        verify(jdbc, never()).update(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("A row Kafka can never take is set aside with its reason; the rows after it still go out")
    void recordFailureIsSetAside() {
        MockProducer<String, String> producer = producer(false);
        AuditRelay relay = relay(producer);
        Thread broker = new Thread(() -> {
            awaitSends(producer, 1);
            producer.errorNext(new RecordTooLargeException("too large"));
            awaitSends(producer, 2);
            producer.completeNext();
            awaitSends(producer, 3);
            producer.completeNext();
        });
        broker.start();

        assertEquals(3, relay.relayOnce());
        verify(jdbc).update(contains("failed_at"), contains("RecordTooLargeException"), eq("row-1"));
        verify(jdbc).update(contains("published_at"), eq("row-2"));
        verify(jdbc).update(contains("published_at"), eq("row-3"));
    }

    @Test
    @DisplayName("While another replica relays, a round claims nothing")
    void oneRelayAtATime() {
        when(jdbc.queryForObject(contains("advisory"), eq(Boolean.class))).thenReturn(false);
        MockProducer<String, String> producer = producer(true);

        assertEquals(0, relay(producer).drain());
        verify(jdbc, never()).queryForList(anyString(), anyInt());
        assertEquals(0, producer.history().size());
    }

    @Test
    @DisplayName("Without the service's name there is no relay: AM would refuse its events")
    void serviceNameRequired() {
        MockProducer<String, String> producer = producer(true);
        Duration timeout = Duration.ofSeconds(1);
        assertThrows(IllegalStateException.class, () -> new AuditRelay(jdbc, transactions, producer, "igrp.audit",
                " ", 100, timeout));
    }

    @Test
    @DisplayName("drain() runs rounds until one is not full")
    void drainRunsFullRounds() {
        MockProducer<String, String> producer = producer(true);
        AuditRelay relay = new AuditRelay(jdbc, transactions, producer, "igrp.audit", "faturacao-api", 3,
                Duration.ofSeconds(1));
        when(jdbc.queryForList(anyString(), anyInt()))
                .thenReturn(List.copyOf(rows))
                .thenReturn(List.of(rows.getFirst()));

        assertEquals(4, relay.drain());
    }

    private AuditRelay relay(MockProducer<String, String> producer) {
        return new AuditRelay(jdbc, transactions, producer, "igrp.audit", "faturacao-api", 100,
                Duration.ofSeconds(2));
    }

    private static MockProducer<String, String> producer(boolean autoComplete) {
        return new MockProducer<>(autoComplete, null, new StringSerializer(), new StringSerializer());
    }

    private static void awaitSends(MockProducer<String, String> producer, int count) {
        long until = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (producer.history().size() < count && System.nanoTime() < until) {
            Thread.onSpinWait();
        }
    }
}
