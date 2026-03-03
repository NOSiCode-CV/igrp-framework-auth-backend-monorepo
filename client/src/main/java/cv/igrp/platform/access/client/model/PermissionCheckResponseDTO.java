package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionCheckResponseDTO {
    @JsonProperty("allowed")
    private boolean allowed;

    @JsonProperty("viaRoles")
    private List<String> viaRoles;

    @JsonProperty("cacheHit")
    private boolean cacheHit;

    @JsonProperty("resolutionTimeMs")
    private long resolutionTimeMs;

    @JsonProperty("reason")
    private String reason;

    // Getters and Setters
    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public List<String> getViaRoles() {
        return viaRoles;
    }

    public void setViaRoles(List<String> viaRoles) {
        this.viaRoles = viaRoles;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public long getResolutionTimeMs() {
        return resolutionTimeMs;
    }

    public void setResolutionTimeMs(long resolutionTimeMs) {
        this.resolutionTimeMs = resolutionTimeMs;
    }

    public String getReason() { return reason; }

    public void setReason(String reason) { this.reason = reason; }
}