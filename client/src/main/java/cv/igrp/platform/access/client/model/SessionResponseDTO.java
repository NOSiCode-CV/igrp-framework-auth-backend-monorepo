package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import cv.igrp.platform.access.client.constants.SessionStatus;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionResponseDTO {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("status")
    private SessionStatus status;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("lastSeenAt")
    private String lastSeenAt;

    @JsonProperty("expiresAt")
    private String expiresAt;

    @JsonProperty("endedAt")
    private String endedAt;

    @JsonProperty("closedReason")
    private String closedReason;

    @JsonProperty("closedBy")
    private String closedBy;

    @JsonProperty("clientIp")
    private String clientIp;

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("userProfile")
    private IGRPUserDTO userProfile;

    @JsonProperty("currentActiveRole")
    private RoleDepartmentDTO currentActiveRole;

    @JsonProperty("roles")
    private List<RoleDepartmentDTO> roles;

    @JsonProperty("departments")
    private List<CodeDescriptionDTO> departments;

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

    public String getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(String lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public String getEndedAt() { return endedAt; }
    public void setEndedAt(String endedAt) { this.endedAt = endedAt; }

    public String getClosedReason() { return closedReason; }
    public void setClosedReason(String closedReason) { this.closedReason = closedReason; }

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public IGRPUserDTO getUserProfile() { return userProfile; }
    public void setUserProfile(IGRPUserDTO userProfile) { this.userProfile = userProfile; }

    public RoleDepartmentDTO getCurrentActiveRole() { return currentActiveRole; }
    public void setCurrentActiveRole(RoleDepartmentDTO currentActiveRole) { this.currentActiveRole = currentActiveRole; }

    public List<RoleDepartmentDTO> getRoles() { return roles; }
    public void setRoles(List<RoleDepartmentDTO> roles) { this.roles = roles; }

    public List<CodeDescriptionDTO> getDepartments() { return departments; }
    public void setDepartments(List<CodeDescriptionDTO> departments) { this.departments = departments; }
}
