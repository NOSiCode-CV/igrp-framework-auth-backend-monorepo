package cv.igrp.platform.access.client;

import cv.igrp.platform.access.client.errors.IgrpErrorCode;
import cv.igrp.platform.access.client.errors.ProblemDetail;

/**
 * Exception thrown by {@link ApiClient} on any non-2xx response from the
 * Access Management API.
 *
 * <p>Backwards-compatible with the previous shape — {@code getCode()} still
 * returns the HTTP status integer and the message still summarises the error.
 * New error-handling fields surface the structured RFC-7807 {@code ProblemDetail}
 * the server now returns on every error (introduced by server commit
 * {@code 31c471ac}):
 *
 * <ul>
 *   <li>{@link #getErrorCode()} — the {@code IGRP_AUTH_*} code string (e.g.
 *       {@code IGRP_AUTH_USER_NOT_FOUND_BY_ID}), {@code null} when the response
 *       didn't carry one;</li>
 *   <li>{@link #getErrorCodeEnum()} — the corresponding {@link IgrpErrorCode}
 *       enum constant when this client knows the code, {@code null} otherwise;</li>
 *   <li>{@link #getTitle()} — the formatted human-readable message returned in
 *       {@code ProblemDetail.title};</li>
 *   <li>{@link #getDetails()} — opaque per-endpoint context attached to the
 *       response under the {@code details} extension;</li>
 *   <li>{@link #getProblemDetail()} — the full {@link ProblemDetail} body
 *       parsed from the response (for callers that need additional RFC-7807
 *       fields like {@code type}, {@code instance}, or other custom extensions).</li>
 * </ul>
 *
 * <p>Idiomatic usage:
 * <pre>{@code
 * try {
 *     usersApi.getUser(userId);
 * } catch (ApiException ex) {
 *     if (IgrpErrorCode.IGRP_AUTH_USER_NOT_FOUND_BY_ID.matches(ex)) {
 *         // typed not-found path
 *     } else {
 *         log.error("API call failed [{}] {}: {}",
 *                   ex.getCode(), ex.getErrorCode(), ex.getTitle());
 *     }
 * }
 * }</pre>
 */
public class ApiException extends Exception {

    private final int code;
    private final ProblemDetail problemDetail;

    public ApiException() {
        this.code = 0;
        this.problemDetail = null;
    }

    public ApiException(int code, String msg) {
        super(msg);
        this.code = code;
        this.problemDetail = null;
    }

    /**
     * Build a rich exception from a parsed {@link ProblemDetail} body. The
     * HTTP status is taken from {@code body.status} when present, otherwise
     * from the {@code httpStatus} argument.
     */
    public ApiException(int httpStatus, ProblemDetail body) {
        super(messageFor(httpStatus, body));
        this.code = body != null && body.getStatus() > 0 ? body.getStatus() : httpStatus;
        this.problemDetail = body;
    }

    public ApiException(Throwable throwable) {
        super(throwable);
        this.code = 0;
        this.problemDetail = null;
    }

    /**
     * @return the HTTP status code returned by the API (e.g. 404, 409, 500).
     *         Returns {@code 0} when the exception wraps a non-HTTP failure
     *         (network error, deserialisation failure, etc.).
     */
    public int getCode() {
        return code;
    }

    /**
     * @return the {@code IGRP_AUTH_*} error code string from the response
     *         body's {@code code} extension, or {@code null} if the server
     *         didn't include one.
     */
    public String getErrorCode() {
        return problemDetail != null ? problemDetail.getCode() : null;
    }

    /**
     * @return the {@link IgrpErrorCode} enum constant matching this error,
     *         or {@code null} if the server returned a code this client
     *         doesn't recognise (e.g. predates the catalogue or is newer
     *         than the client release).
     */
    public IgrpErrorCode getErrorCodeEnum() {
        return IgrpErrorCode.fromString(getErrorCode());
    }

    /**
     * @return the formatted message from {@code ProblemDetail.title}.
     *         May be {@code null} for legacy responses.
     */
    public String getTitle() {
        return problemDetail != null ? problemDetail.getTitle() : null;
    }

    /**
     * @return per-endpoint extension context attached to the response under
     *         {@code details}, or {@code null} when absent.
     */
    public Object getDetails() {
        return problemDetail != null ? problemDetail.getDetails() : null;
    }

    /**
     * @return the full parsed RFC-7807 body of the response, or {@code null}
     *         when the response could not be parsed (e.g. plain-text error
     *         from a layer ahead of the API, like a reverse-proxy 502).
     */
    public ProblemDetail getProblemDetail() {
        return problemDetail;
    }

    private static String messageFor(int httpStatus, ProblemDetail body) {
        if (body == null) {
            return "API Error: status=" + httpStatus;
        }
        StringBuilder sb = new StringBuilder("API Error: status=")
                .append(body.getStatus() > 0 ? body.getStatus() : httpStatus);
        if (body.getCode() != null) {
            sb.append(", code=").append(body.getCode());
        }
        if (body.getTitle() != null) {
            sb.append(", title=").append(body.getTitle());
        }
        return sb.toString();
    }
}
