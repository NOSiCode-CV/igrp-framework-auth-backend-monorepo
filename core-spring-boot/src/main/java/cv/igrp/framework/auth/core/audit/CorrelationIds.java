package cv.igrp.framework.auth.core.audit;

import org.slf4j.MDC;
import org.springframework.util.ClassUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The id that joins an audit row, an Envers revision, the logs and the trace in
 * Tempo (v3-24). It is the OTel trace id; without tracing, a valid
 * {@code X-Correlation-ID} header; otherwise a random id.
 *
 * <p>The header is caller-controlled and ends up in audit rows and logs, so it
 * is only accepted when it is 1 to 64 letters, digits, {@code -} or {@code _}.
 *
 * <p>OpenTelemetry is optional: the API is reached only through {@link Otel},
 * which is loaded only when the API is on the classpath.
 */
public final class CorrelationIds {

    public static final String HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";
    public static final String TRACEPARENT = "traceparent";

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final boolean OTEL_PRESENT =
            ClassUtils.isPresent("io.opentelemetry.api.trace.Span", CorrelationIds.class.getClassLoader());

    private CorrelationIds() {
    }

    /** The correlation id for a request that sent {@code headerValue} (may be null). */
    public static String resolve(String headerValue) {
        return traceId().orElseGet(() -> isValid(headerValue) ? headerValue : random());
    }

    /** The id bound to the current request, else the current trace id; empty outside both. */
    public static Optional<String> current() {
        String bound = MDC.get(MDC_KEY);
        return bound != null ? Optional.of(bound) : traceId();
    }

    /**
     * Headers for a call to another service: {@code traceparent} when a span is
     * current, and {@code X-Correlation-ID} when an id is known.
     */
    public static Map<String, String> outgoingHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        if (OTEL_PRESENT) {
            Otel.injectTraceparent(headers);
        }
        current().ifPresent(id -> headers.put(HEADER, id));
        return headers;
    }

    static boolean isValid(String value) {
        return value != null && VALID.matcher(value).matches();
    }

    private static Optional<String> traceId() {
        return OTEL_PRESENT ? Otel.traceId() : Optional.empty();
    }

    private static String random() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** Every OpenTelemetry reference lives here, so the class loads only when the API is present. */
    private static final class Otel {

        private static Optional<String> traceId() {
            io.opentelemetry.api.trace.SpanContext span =
                    io.opentelemetry.api.trace.Span.current().getSpanContext();
            return span.isValid() ? Optional.of(span.getTraceId()) : Optional.empty();
        }

        private static void injectTraceparent(Map<String, String> headers) {
            io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator.getInstance()
                    .inject(io.opentelemetry.context.Context.current(), headers, Map::put);
        }
    }
}
