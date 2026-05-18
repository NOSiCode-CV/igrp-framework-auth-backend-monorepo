package cv.igrp.platform.access.client.errors;

/**
 * Mirror of the Access Management API's centralised error code catalogue
 * (server class {@code cv.igrp.platform.access_management.shared.domain.exceptions.IgrpErrorCode}).
 *
 * <p>Each constant carries:
 * <ul>
 *   <li>the stable, descriptive code (the enum name, e.g.
 *       {@link #IGRP_AUTH_USER_NOT_FOUND_BY_ID}) — exposed in the {@code code}
 *       property of the API's {@code ProblemDetail} response body;</li>
 *   <li>the canonical HTTP status the server returns for that code (mirrors
 *       the server-side binding; the client trusts the actual response status
 *       on the wire and uses this field for ergonomics only).</li>
 * </ul>
 *
 * <p>Match against {@code ApiException.getErrorCode()}:
 * <pre>{@code
 * } catch (ApiException ex) {
 *     if (IgrpErrorCode.IGRP_AUTH_USER_NOT_FOUND_BY_ID.matches(ex)) {
 *         // ... handle not-found path ...
 *     }
 * }
 * }</pre>
 *
 * <p>The Javadoc-only summary for each code preserves the server-side message
 * template (with {@code {0}}, {@code {1}}, ... placeholders) so consumers can
 * see the contract; the actual formatted message at runtime comes from
 * {@code ApiException.getTitle()}.
 */
public enum IgrpErrorCode {

    // ─── User ────────────────────────────────────────────────────────────────
    IGRP_AUTH_USER_NOT_FOUND_BY_ID(404),
    IGRP_AUTH_USER_NOT_FOUND_BY_EXTERNAL_ID(401),
    IGRP_AUTH_USER_NOT_FOUND_BY_USERNAME(404),
    IGRP_AUTH_USER_INACTIVE(403),

    // ─── Application ─────────────────────────────────────────────────────────
    IGRP_AUTH_APPLICATION_NOT_FOUND_BY_CODE(404),
    IGRP_AUTH_APPLICATION_NOT_FOUND_BY_ID(404),
    IGRP_AUTH_APPLICATION_CODE_REQUIRED(400),
    IGRP_AUTH_APPLICATION_VALIDATION_FAILED(409),
    IGRP_AUTH_APPLICATION_CREATION_FAILED(500),
    IGRP_AUTH_APPLICATION_SYNC_FAILED(500),

    // ─── Department ──────────────────────────────────────────────────────────
    IGRP_AUTH_DEPARTMENT_NOT_FOUND_BY_ID(404),
    IGRP_AUTH_DEPARTMENT_NOT_FOUND_BY_CODE(404),
    IGRP_AUTH_DEPARTMENT_INACTIVE(400),
    IGRP_AUTH_DEPARTMENT_INVALID_STATUS(400),
    IGRP_AUTH_DEPARTMENT_CODE_ALREADY_EXISTS(400),
    IGRP_AUTH_DEPARTMENT_PARENT_NOT_FOUND(404),
    IGRP_AUTH_DEPARTMENT_ACCESS_DENIED(400),
    IGRP_AUTH_DEPARTMENT_PARENT_APPLICATION_NOT_ASSIGNED(403),

    // ─── Role ────────────────────────────────────────────────────────────────
    IGRP_AUTH_ROLE_NOT_FOUND_BY_ID(404),
    IGRP_AUTH_ROLE_NOT_FOUND_BY_CODE(404),
    IGRP_AUTH_ROLE_INACTIVE(400),
    IGRP_AUTH_ROLES_NOT_FOUND(404),
    IGRP_AUTH_PARENT_ROLE_NOT_FOUND(404),
    IGRP_AUTH_CHILD_ROLE_NOT_FOUND(404),
    IGRP_AUTH_ROLE_VALIDATION_FAILED(409),

    // ─── Menu ────────────────────────────────────────────────────────────────
    IGRP_AUTH_MENU_NOT_FOUND_BY_CODE(404),
    IGRP_AUTH_MENU_ENTRY_NOT_FOUND(404),
    IGRP_AUTH_MENU_ENTRY_NOT_FOUND_FOR_APPLICATION(404),
    IGRP_AUTH_MENU_ENTRY_DTO_MISSING(400),
    IGRP_AUTH_MENU_PAGE_SLUG_REQUIRED_FOR_SYSTEM(400),
    IGRP_AUTH_MENU_PAGE_SLUG_REQUIRED_FOR_PAGE(400),
    IGRP_AUTH_MENU_URL_REQUIRED_FOR_EXTERNAL(400),
    IGRP_AUTH_MENU_PARENT_NOT_FOUND(404),
    IGRP_AUTH_MENU_VALIDATION_FAILED(409),
    IGRP_AUTH_MENU_NOT_ASSOCIATED_TO_PARENT_DEPARTMENT(400),

    // ─── Permission ──────────────────────────────────────────────────────────
    IGRP_AUTH_PERMISSION_NOT_FOUND_BY_NAME(400),
    IGRP_AUTH_PERMISSIONS_NOT_FOUND(404),
    IGRP_AUTH_PERMISSION_VALID_LIST_EMPTY(400),

    // ─── Resource ────────────────────────────────────────────────────────────
    IGRP_AUTH_RESOURCE_NOT_FOUND_BY_NAME(404),

    // ─── Invitation / OTP ───────────────────────────────────────────────────
    IGRP_AUTH_INVITATION_NOT_FOUND_BY_ID(404),
    IGRP_AUTH_INVITATION_NOT_FOUND_BY_TOKEN(404),
    IGRP_AUTH_INVITATION_TOKEN_EXPIRED(400),
    IGRP_AUTH_INVITATION_EMAIL_REQUIRED(400),
    IGRP_AUTH_INVITATION_EMAIL_MISMATCH(400),
    IGRP_AUTH_INVITATION_STATUS_INVALID(400),
    IGRP_AUTH_INVITATION_RESPONSE_UNAUTHORIZED(401),
    IGRP_AUTH_INVITATION_OTP_NOT_FOUND(404),
    IGRP_AUTH_INVITATION_OTP_EXPIRED(400),
    IGRP_AUTH_INVITATION_OTP_INVALID(400),
    IGRP_AUTH_INVITATION_OTP_NOT_VALIDATED(400),
    IGRP_AUTH_INVITATION_OTP_SEND_FAILED(500),
    IGRP_AUTH_INVITATION_ALREADY_ACCEPTED(409),
    IGRP_AUTH_INVITATION_ALREADY_REJECTED(409),
    IGRP_AUTH_INVITATION_NOT_PENDING(409),

    // ─── Token / Subject ────────────────────────────────────────────────────
    IGRP_AUTH_INVALID_TOKEN_SUBJECT(401),

    // ─── Favorite Application ────────────────────────────────────────────────
    IGRP_AUTH_FAVORITE_APPLICATION_NOT_FOUND_BY_ID(404),
    IGRP_AUTH_FAVORITE_APPLICATION_ALREADY_EXISTS(400),
    IGRP_AUTH_FAVORITE_APPLICATION_NOT_FAVORITED(400),
    IGRP_AUTH_FAVORITE_APPLICATION_NONE_FOR_USER(404),

    // ─── Access History ──────────────────────────────────────────────────────
    IGRP_AUTH_ACCESS_HISTORY_NOT_FOUND_BY_ID(404),

    // ─── Custom Fields ───────────────────────────────────────────────────────
    IGRP_AUTH_CUSTOM_FIELD_NOT_FOUND_FOR_APPLICATION(404),

    // ─── Global Configuration ────────────────────────────────────────────────
    IGRP_AUTH_GLOBAL_CONFIGURATION_TYPE_NOT_FOUND(400),
    IGRP_AUTH_GLOBAL_CONFIGURATION_NOT_FOUND(404),

    // ─── File ────────────────────────────────────────────────────────────────
    IGRP_AUTH_FILE_REQUIRED(400),
    IGRP_AUTH_FILE_PATH_REQUIRED(400),
    IGRP_AUTH_FILE_PRIVATE_URL_GENERATION_FAILED(400),
    IGRP_AUTH_FILE_UPLOAD_FAILED(400),

    // ─── Session ─────────────────────────────────────────────────────────────
    IGRP_AUTH_SESSION_USER_NOT_FOUND(404),
    IGRP_AUTH_SESSION_USER_INACTIVE(403),

    // ─── M2M ─────────────────────────────────────────────────────────────────
    IGRP_AUTH_APP_CENTER_URL_REQUIRED(400),

    // ─── Generic ─────────────────────────────────────────────────────────────
    IGRP_AUTH_INTERNAL_SERVER_ERROR(500),
    IGRP_AUTH_BAD_REQUEST(400),
    IGRP_AUTH_FORBIDDEN(403),
    IGRP_AUTH_NOT_FOUND(404);

    private final int httpStatus;

    IgrpErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    /**
     * @return the canonical HTTP status the server returns for this code.
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * @return {@code true} when the given exception was raised with this code.
     *         Returns {@code false} for legacy errors that pre-date the
     *         {@code IgrpErrorCode} catalogue or for non-{@code ApiException}
     *         throwables.
     */
    public boolean matches(Throwable ex) {
        if (ex instanceof cv.igrp.platform.access.client.ApiException apiEx) {
            return name().equals(apiEx.getErrorCode());
        }
        return false;
    }

    /**
     * Lenient lookup: returns the enum constant matching the given code
     * string, or {@code null} when the server returned an unknown code
     * (e.g. the client predates a newly-introduced server code).
     */
    public static IgrpErrorCode fromString(String code) {
        if (code == null) {
            return null;
        }
        try {
            return IgrpErrorCode.valueOf(code);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
