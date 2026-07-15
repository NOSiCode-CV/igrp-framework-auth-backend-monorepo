package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of a gated audit-log purge ({@code DELETE /api/auth/audit/purge}).
 * {@link #getPurged()} is the number of rows removed (the GENESIS anchor is
 * retained and the chain restarts from it).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditPurgeResultDTO {

    @JsonProperty("purged")
    private long purged;

    public long getPurged() { return purged; }
    public void setPurged(long purged) { this.purged = purged; }
}
