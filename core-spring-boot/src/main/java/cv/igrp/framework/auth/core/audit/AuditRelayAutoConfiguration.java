package cv.igrp.framework.auth.core.audit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * v3-22 (#32): the outbox relay, for a service whose business audit is on and
 * that names its Kafka brokers in {@code igrp.audit.relay.bootstrap-servers}
 * ({@code IGRP_AUDIT_RELAY_BOOTSTRAP_SERVERS}). Kafka's client is an optional
 * dependency: a service without it, or without the property, gets no relay and
 * its rows simply stay in the outbox.
 *
 * <ul>
 *   <li>{@code igrp.audit.relay.topic}: {@code igrp.audit};</li>
 *   <li>{@code igrp.audit.relay.interval}: how often a round starts, 5s;</li>
 *   <li>{@code igrp.audit.relay.batch}: rows per round, 100;</li>
 *   <li>{@code igrp.audit.relay.send-timeout}: how long one row waits for its acknowledgement, 10s.</li>
 * </ul>
 */
@org.springframework.boot.autoconfigure.AutoConfiguration(after = AuditAutoConfiguration.class)
@ConditionalOnClass(name = "org.apache.kafka.clients.producer.KafkaProducer")
@ConditionalOnProperty(name = "igrp.audit.relay.bootstrap-servers")
@ConditionalOnBean(Audit.class)
public class AuditRelayAutoConfiguration {

    /** Every replica acknowledged, sent once even on retries: an acknowledged row is durable. */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(name = "igrpAuditProducer")
    public Producer<String, String> igrpAuditProducer(
            @Value("${igrp.audit.relay.bootstrap-servers}") String bootstrapServers,
            @Value("${igrp.audit.relay.send-timeout:10s}") Duration sendTimeout) {
        int timeout = Math.toIntExact(sendTimeout.toMillis());
        return new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.MAX_BLOCK_MS_CONFIG, timeout,
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, timeout,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, timeout,
                ProducerConfig.LINGER_MS_CONFIG, 0,
                ProducerConfig.CLIENT_ID_CONFIG, "igrp-audit-relay"),
                new StringSerializer(), new StringSerializer());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditRelay igrpAuditRelay(DataSource dataSource, PlatformTransactionManager transactionManager,
                                     Producer<String, String> igrpAuditProducer,
                                     @Value("${igrp.audit.relay.topic:igrp.audit}") String topic,
                                     @Value("${igrp.security.service-name:${spring.application.name:}}") String service,
                                     @Value("${igrp.audit.relay.batch:100}") int batch,
                                     @Value("${igrp.audit.relay.send-timeout:10s}") Duration sendTimeout) {
        return new AuditRelay(new JdbcTemplate(dataSource), new TransactionTemplate(transactionManager),
                igrpAuditProducer, topic, service, batch, sendTimeout);
    }

    @Bean
    public AuditRelaySchedule igrpAuditRelaySchedule(AuditRelay relay,
                                                     @Value("${igrp.audit.relay.interval:5s}") Duration interval) {
        return new AuditRelaySchedule(relay, interval);
    }

    /** Runs the relay on its own thread, a round every interval, from startup to shutdown. */
    public static final class AuditRelaySchedule implements SmartLifecycle {

        private static final Logger LOGGER = LoggerFactory.getLogger(AuditRelaySchedule.class);

        private final AuditRelay relay;
        private final Duration interval;
        private ScheduledExecutorService executor;

        AuditRelaySchedule(AuditRelay relay, Duration interval) {
            this.relay = relay;
            this.interval = interval;
        }

        @Override
        public synchronized void start() {
            executor = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "igrp-audit-relay");
                thread.setDaemon(true);
                return thread;
            });
            executor.scheduleWithFixedDelay(this::round, interval.toMillis(), interval.toMillis(),
                    TimeUnit.MILLISECONDS);
        }

        private void round() {
            try {
                relay.drain();
            } catch (RuntimeException ex) {
                // A database outage, say: the next round tries again; the schedule must not die.
                LOGGER.error("[AUDIT] Outbox relay round failed", ex);
            }
        }

        @Override
        public synchronized void stop() {
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
        }

        @Override
        public synchronized boolean isRunning() {
            return executor != null;
        }
    }
}
