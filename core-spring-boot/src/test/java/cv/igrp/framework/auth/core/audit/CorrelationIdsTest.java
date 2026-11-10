package cv.igrp.framework.auth.core.audit;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * v3-24 (#31): the correlation id is the OTel trace id, falling back to a valid
 * {@code X-Correlation-ID}, then a random id.
 */
class CorrelationIdsTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void theTraceIdWinsOverTheHeader() {
        try (Scope ignored = activeSpan()) {
            assertThat(CorrelationIds.resolve("from-header")).isEqualTo(TRACE_ID);
        }
    }

    @Test
    void withoutTracingAValidHeaderIsUsed() {
        assertThat(CorrelationIds.resolve("order-42_retry-1")).isEqualTo("order-42_retry-1");
    }

    @Test
    void withoutTracingOrHeaderTheIdIsRandom() {
        String first = CorrelationIds.resolve(null);
        String second = CorrelationIds.resolve("  ");

        assertThat(first).matches("[0-9a-f]{32}");
        assertThat(second).matches("[0-9a-f]{32}").isNotEqualTo(first);
    }

    @Test
    void aHeaderThatCouldInjectIntoLogsOrOverflowTheColumnIsReplaced() {
        assertThat(CorrelationIds.resolve("abc\r\nINFO forged log line")).matches("[0-9a-f]{32}");
        assertThat(CorrelationIds.resolve("x".repeat(65))).matches("[0-9a-f]{32}");
        assertThat(CorrelationIds.resolve("has space")).matches("[0-9a-f]{32}");
        assertThat(CorrelationIds.resolve("x".repeat(64))).isEqualTo("x".repeat(64));
    }

    @Test
    void theCurrentIdIsTheOneBoundToTheRequest() {
        MDC.put(CorrelationIds.MDC_KEY, "bound-to-request");

        assertThat(CorrelationIds.current()).contains("bound-to-request");
    }

    @Test
    void outsideARequestTheCurrentIdIsTheTraceIdIfAny() {
        assertThat(CorrelationIds.current()).isEmpty();
        try (Scope ignored = activeSpan()) {
            assertThat(CorrelationIds.current()).contains(TRACE_ID);
        }
    }

    @Test
    void outgoingCallsCarryTraceparentAndTheCorrelationId() {
        try (Scope ignored = activeSpan()) {
            MDC.put(CorrelationIds.MDC_KEY, TRACE_ID);

            Map<String, String> headers = CorrelationIds.outgoingHeaders();

            assertThat(headers).containsEntry(CorrelationIds.HEADER, TRACE_ID)
                    .containsEntry("traceparent", "00-" + TRACE_ID + "-" + SPAN_ID + "-01");
        }
    }

    @Test
    void withoutTracingOutgoingCallsCarryOnlyTheCorrelationId() {
        MDC.put(CorrelationIds.MDC_KEY, "order-42");

        assertThat(CorrelationIds.outgoingHeaders()).containsExactly(Map.entry(CorrelationIds.HEADER, "order-42"));
    }

    @Test
    void withNothingKnownOutgoingCallsCarryNoHeaders() {
        assertThat(CorrelationIds.outgoingHeaders()).isEmpty();
    }

    private static Scope activeSpan() {
        return Span.wrap(SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault()))
                .makeCurrent();
    }
}
