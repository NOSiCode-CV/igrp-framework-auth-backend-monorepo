package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionKillRequestDTO {

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("killedBy")
    private String killedBy;

    public SessionKillRequestDTO() {}

    public SessionKillRequestDTO(String reason, String killedBy) {
        this.reason = reason;
        this.killedBy = killedBy;
    }

    // Getters and Setters
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getKilledBy() { return killedBy; }
    public void setKilledBy(String killedBy) { this.killedBy = killedBy; }
}
