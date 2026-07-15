package cv.igrp.platform.access.client.model.reports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * One row of the Access Report ({@code GET /api/auth/reports/access}). Field
 * names mirror the server DTO
 * {@code cv.igrp.platform.access_management.security_audit.application.dto.AccessReportRowDTO}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessReportRowDTO {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("username")
    private String username;

    @JsonProperty("role")
    private String role;

    @JsonProperty("module")
    private String module;

    @JsonProperty("action")
    private String action;

    @JsonProperty("ipAddress")
    private String ipAddress;

    @JsonProperty("status")
    private AuditStatus status;

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public AuditStatus getStatus() { return status; }
    public void setStatus(AuditStatus status) { this.status = status; }
}
