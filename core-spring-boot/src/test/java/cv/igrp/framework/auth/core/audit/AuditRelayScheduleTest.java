package cv.igrp.framework.auth.core.audit;

import cv.igrp.framework.auth.core.audit.AuditRelayAutoConfiguration.AuditRelaySchedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/** v3-22 (#32): the relay's own thread runs a round every interval, survives a failed one, and stops cleanly. */
class AuditRelayScheduleTest {

    @Test
    @DisplayName("A failed round does not stop the schedule; the next round runs")
    void survivesAFailedRound() throws InterruptedException {
        AuditRelay relay = mock(AuditRelay.class);
        AtomicInteger rounds = new AtomicInteger();
        CountDownLatch twoRounds = new CountDownLatch(2);
        doAnswer(call -> {
            twoRounds.countDown();
            if (rounds.incrementAndGet() == 1) {
                throw new IllegalStateException("database down");
            }
            return 0;
        }).when(relay).drain();
        AuditRelaySchedule schedule = new AuditRelaySchedule(relay, Duration.ofMillis(10));

        schedule.start();
        try {
            assertThat(schedule.isRunning()).isTrue();
            assertThat(twoRounds.await(5, TimeUnit.SECONDS)).as("a round after the failed one").isTrue();
        } finally {
            schedule.stop();
        }
        assertThat(schedule.isRunning()).isFalse();
    }

    @Test
    @DisplayName("Stopping waits for the round under way to finish")
    void stopWaitsForTheRound() throws InterruptedException {
        AuditRelay relay = mock(AuditRelay.class);
        CountDownLatch started = new CountDownLatch(1);
        AtomicInteger finished = new AtomicInteger();
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(call -> {
            started.countDown();
            // Still under way when stop() is called; finishes once stop() is waiting.
            release.await(5, TimeUnit.SECONDS);
            finished.incrementAndGet();
            return 0;
        }).when(relay).drain();
        AuditRelaySchedule schedule = new AuditRelaySchedule(relay, Duration.ofMillis(10));
        schedule.start();
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        Thread releaser = new Thread(release::countDown);
        releaser.start();
        schedule.stop();
        releaser.join();

        assertThat(finished.get()).as("the round under way completed before stop returned").isPositive();
    }

    @Test
    @DisplayName("Stopping a schedule that never started does nothing")
    void stopBeforeStart() {
        AuditRelaySchedule schedule = new AuditRelaySchedule(mock(AuditRelay.class), Duration.ofHours(1));

        schedule.stop();

        assertThat(schedule.isRunning()).isFalse();
    }
}
