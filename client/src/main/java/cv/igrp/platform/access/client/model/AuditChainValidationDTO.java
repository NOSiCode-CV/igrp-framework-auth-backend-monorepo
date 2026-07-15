package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of an on-demand tamper-evidence check
 * ({@code POST /api/auth/audit/validate}). When {@link #isValid()} is
 * {@code false}, {@link #getBrokenAt()} carries the {@code sequence_number} of
 * the first row whose hash did not verify.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditChainValidationDTO {

    @JsonProperty("valid")
    private boolean valid;

    @JsonProperty("rows_checked")
    private long rowsChecked;

    /** {@code null} when the chain is intact. */
    @JsonProperty("broken_at")
    private Long brokenAt;

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public long getRowsChecked() { return rowsChecked; }
    public void setRowsChecked(long rowsChecked) { this.rowsChecked = rowsChecked; }

    public Long getBrokenAt() { return brokenAt; }
    public void setBrokenAt(Long brokenAt) { this.brokenAt = brokenAt; }
}
