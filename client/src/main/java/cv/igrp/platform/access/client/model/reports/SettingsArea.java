package cv.igrp.platform.access.client.model.reports;

/**
 * Administrative area a settings/configuration event belongs to. Backs the
 * Settings Report {@code area} filter and column.
 *
 * <p>Mirrors the server enum
 * {@code cv.igrp.platform.access_management.security_audit.domain.enums.SettingsArea}.
 */
public enum SettingsArea {
    APPLICATIONS,
    USERS,
    ACCESS
}
