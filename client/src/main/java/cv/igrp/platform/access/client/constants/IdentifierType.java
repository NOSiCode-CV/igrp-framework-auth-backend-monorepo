package cv.igrp.platform.access.client.constants;

/**
 * Type of identifier an invitation / audit row carries for the invited /
 * acting principal. Mirrors the server enum
 * {@code cv.igrp.platform.access_management.shared.application.constants.IdentifierType}.
 *
 * <p>Wire format is the enum {@code name()} (e.g. {@code "EMAIL"}).
 * Unknown server-side additions surface as {@link #UNKNOWN} thanks to the
 * {@link com.fasterxml.jackson.annotation.JsonIgnoreProperties} on the
 * model DTO instead of throwing a deserialization error.
 */
public enum IdentifierType {
    CNI,
    PHONE,
    EMAIL,
    UNKNOWN
}
