package cv.igrp.platform.access.client.errors;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mirror of <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC 7807</a>
 * {@code ProblemDetail} as returned by the Access Management API's
 * {@code GlobalExceptionHandler} on every error response.
 *
 * <p>Standard RFC-7807 members are exposed as typed fields ({@link #getType()},
 * {@link #getTitle()}, {@link #getStatus()}, {@link #getDetail()},
 * {@link #getInstance()}). API-specific extensions land in
 * {@link #getProperties()} and are surfaced via convenience getters:
 * <ul>
 *   <li>{@link #getCode()} — the {@code IGRP_AUTH_*} error code string;</li>
 *   <li>{@link #getDetails()} — opaque extra context attached by the server
 *       ({@link Object} because the shape is endpoint-specific).</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProblemDetail {

    /** RFC-7807 property name used by the server to expose the IgrpErrorCode. */
    public static final String CODE_PROPERTY = "code";

    /** RFC-7807 property name used by the server to attach opaque per-error context. */
    public static final String DETAILS_PROPERTY = "details";

    private URI type;
    private String title;
    private int status;
    private String detail;
    private URI instance;

    private final Map<String, Object> properties = new LinkedHashMap<>();

    public URI getType() { return type; }
    public void setType(URI type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public URI getInstance() { return instance; }
    public void setInstance(URI instance) { this.instance = instance; }

    /**
     * @return the full set of RFC-7807 extension properties on the response
     *         (including {@code code} and {@code details} which are also
     *         exposed via the typed accessors below).
     */
    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }

    /** Jackson sink for any property that isn't a standard RFC-7807 field. */
    @JsonAnySetter
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    /**
     * Convenience accessor for the {@code code} extension — the IGRP_AUTH_*
     * error code string set by the server's {@code IgrpErrorCode}.
     *
     * @return the code string, or {@code null} for legacy responses that
     *         pre-date the IgrpErrorCode catalogue.
     */
    public String getCode() {
        Object value = properties.get(CODE_PROPERTY);
        return value != null ? value.toString() : null;
    }

    /**
     * Convenience accessor for the {@code details} extension. Returns
     * {@code null} when absent. The shape is endpoint-specific (a Map,
     * a String, a List, etc.) — callers must cast as appropriate.
     */
    public Object getDetails() {
        return properties.get(DETAILS_PROPERTY);
    }
}
