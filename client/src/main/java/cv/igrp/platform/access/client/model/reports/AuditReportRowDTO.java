package cv.igrp.platform.access.client.model.reports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * One row of the Audit Report ({@code GET /api/auth/reports/audit}). Field names
 * and nullability mirror the server DTO
 * {@code cv.igrp.platform.access_management.security_audit.application.dto.AuditReportRowDTO}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditReportRowDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("startDate")
    private Instant startDate;

    @JsonProperty("endDate")
    private Instant endDate;

    @JsonProperty("username")
    private String username;

    @JsonProperty("module")
    private String module;

    @JsonProperty("accessRole")
    private String accessRole;

    @JsonProperty("operationState")
    private String operationState;

    @JsonProperty("ipAddress")
    private String ipAddress;

    /** Derived server-side from the {@code user_agent} header (e.g. {@code "Chrome/Windows"}). */
    @JsonProperty("device")
    private String device;

    @JsonProperty("authorizedBy")
    private String authorizedBy;

    @JsonProperty("status")
    private AuditStatus status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instant getStartDate() { return startDate; }
    public void setStartDate(Instant startDate) { this.startDate = startDate; }

    public Instant getEndDate() { return endDate; }
    public void setEndDate(Instant endDate) { this.endDate = endDate; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getAccessRole() { return accessRole; }
    public void setAccessRole(String accessRole) { this.accessRole = accessRole; }

    public String getOperationState() { return operationState; }
    public void setOperationState(String operationState) { this.operationState = operationState; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public String getAuthorizedBy() { return authorizedBy; }
    public void setAuthorizedBy(String authorizedBy) { this.authorizedBy = authorizedBy; }

    public AuditStatus getStatus() { return status; }
    public void setStatus(AuditStatus status) { this.status = status; }
}
