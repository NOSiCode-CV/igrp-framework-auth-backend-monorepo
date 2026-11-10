package cv.igrp.sdktest.harness;

import cv.igrp.framework.auth.core.audit.CorrelationIds;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v3-24 (#31, AUD-06): every request through the starter carries one
 * correlation id, in the logs, in the response, and in the calls it makes to AM.
 */
class CorrelationIdHarnessTest extends AbstractStarterTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";

    @Test
    @DisplayName("A valid X-Correlation-ID is bound to the request's logs and echoed in the response")
    void validHeaderIsBoundAndEchoed() throws Exception {
        mvc.perform(get("/correlation").header(CorrelationIds.HEADER, "order-42").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("order-42"))
                .andExpect(header().string(CorrelationIds.HEADER, "order-42"));

        assertThat(MDC.get(CorrelationIds.MDC_KEY)).as("cleared after the request").isNull();
    }

    @Test
    @DisplayName("With tracing on, the trace id is the correlation id, whatever the header says")
    void traceIdWins() throws Exception {
        try (Scope ignored = activeSpan()) {
            mvc.perform(get("/correlation").header(CorrelationIds.HEADER, "order-42").with(jwt()))
                    .andExpect(content().string(TRACE_ID))
                    .andExpect(header().string(CorrelationIds.HEADER, TRACE_ID));
        }
    }

    @Test
    @DisplayName("An invalid or missing header gets a random id")
    void invalidOrMissingHeaderGetsRandomId() throws Exception {
        MvcResult forged = mvc.perform(get("/correlation").header(CorrelationIds.HEADER, "a b\tc").with(jwt()))
                .andReturn();
        MvcResult none = mvc.perform(get("/correlation").with(jwt())).andReturn();

        assertThat(forged.getResponse().getHeader(CorrelationIds.HEADER)).matches("[0-9a-f]{32}");
        assertThat(none.getResponse().getContentAsString()).matches("[0-9a-f]{32}")
                .isEqualTo(none.getResponse().getHeader(CorrelationIds.HEADER));
    }

    @Test
    @DisplayName("A call to AM made while serving a request carries traceparent and X-Correlation-ID")
    void callsToAmCarryTheIds() throws Exception {
        try (Scope ignored = activeSpan()) {
            mvc.perform(get("/correlation/call-am").with(jwt())).andExpect(status().isOk());
        }

        StubAccessManagement.Request call = AM.lastRequest("GET", "/api/echo");
        assertThat(call.header(CorrelationIds.HEADER)).isEqualTo(TRACE_ID);
        assertThat(call.header(CorrelationIds.TRACEPARENT)).isEqualTo("00-" + TRACE_ID + "-" + SPAN_ID + "-01");
    }

    @Test
    @DisplayName("Without tracing, a call to AM still carries the request's X-Correlation-ID")
    void callsToAmCarryTheHeaderWithoutTracing() throws Exception {
        mvc.perform(get("/correlation/call-am").header(CorrelationIds.HEADER, "order-43").with(jwt()))
                .andExpect(status().isOk());

        StubAccessManagement.Request call = AM.lastRequest("GET", "/api/echo");
        assertThat(call.header(CorrelationIds.HEADER)).isEqualTo("order-43");
        assertThat(call.header(CorrelationIds.TRACEPARENT)).isNull();
    }

    private static Scope activeSpan() {
        return Span.wrap(SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault()))
                .makeCurrent();
    }
}
