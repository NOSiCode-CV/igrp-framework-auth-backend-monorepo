package cv.igrp.platform.access.client.model.reports;

/**
 * Type of entity a settings/configuration event targets. Backs the Settings
 * Report {@code entityType} filter and column.
 *
 * <p>Mirrors the server enum
 * {@code cv.igrp.platform.access_management.security_audit.domain.enums.SettingsEntityType}.
 */
public enum SettingsEntityType {
    APPLICATION,
    USER,
    DEPARTMENT,
    ROLE,
    PERMISSION,
    MENU
}
