package cv.igrp.platform.access.client.model.reports;

/**
 * Which of the three reports an archived export was generated from.
 *
 * <p>Mirrors the server enum
 * {@code cv.igrp.platform.access_management.security_audit.domain.enums.ReportType}.
 */
public enum ReportType {
    AUDIT,
    ACCESS,
    SETTINGS
}
