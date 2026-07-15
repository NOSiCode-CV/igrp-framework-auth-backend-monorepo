package cv.igrp.platform.access.client.model.reports;

/**
 * Outcome of an audited operation, surfaced on the report {@code status} column.
 *
 * <p>Mirrors the server enum
 * {@code cv.igrp.platform.access_management.security_audit.domain.enums.AuditStatus}.
 * The three report DTOs each expose a documented subset of these values; new
 * server-side values would surface in the JSON as a string outside this enum,
 * so treat unknown values defensively when consuming raw responses.
 */
public enum AuditStatus {
    SUCCESS,
    ACCESS_DENIED,
    UNUSUAL_IP,
    PENDING,
    ERROR
}
