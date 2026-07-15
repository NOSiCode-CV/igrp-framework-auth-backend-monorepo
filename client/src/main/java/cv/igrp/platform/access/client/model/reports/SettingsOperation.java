package cv.igrp.platform.access.client.model.reports;

/**
 * Operation performed by a settings/configuration event. Backs the Settings
 * Report {@code operation} filter and column.
 *
 * <p>Symmetry rule: every additive operation has its inverse —
 * {@code CREATE↔DELETE}, {@code ACTIVATE↔DEACTIVATE},
 * {@code ASSOCIATE↔DISASSOCIATE}, {@code ASSIGN↔UNASSIGN},
 * {@code INVITE↔CANCEL_INVITE}. {@code RESEND_INVITE} is repeatable and has no
 * inverse.
 *
 * <p>Mirrors the server enum
 * {@code cv.igrp.platform.access_management.security_audit.domain.enums.SettingsOperation}.
 */
public enum SettingsOperation {
    CREATE,
    DELETE,
    EDIT,
    ACTIVATE,
    DEACTIVATE,
    INVITE,
    CANCEL_INVITE,
    RESEND_INVITE,
    ASSOCIATE,
    DISASSOCIATE,
    ASSIGN,
    UNASSIGN
}
