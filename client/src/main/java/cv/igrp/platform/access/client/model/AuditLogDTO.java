package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One entry of the unified, tamper-evident audit log, as exposed by
 * {@code GET /api/auth/audit}.
 *
 * <p>Renamed from {@code SecurityAuditLogDTO} in the Unified Audit &amp; Reports
 * feature (R8.3). The primary key is now a UUID (string) and the row carries the
 * hash-chain fields ({@link #getSequenceNumber()}, {@link #getPreviousHash()},
 * {@link #getCurrentHash()}). Mirrors the server DTO
 * {@code cv.igrp.platform.access_management.security_audit.application.dto.SecurityAuditLogDTO}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditLogDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("sequenceNumber")
    private Long sequenceNumber;

    @JsonProperty("previousHash")
    private String previousHash;

    @JsonProperty("currentHash")
    private String currentHash;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("ipAddress")
    private String ipAddress;

    @JsonProperty("userAgent")
    private String userAgent;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("requestPath")
    private String requestPath;

    @JsonProperty("decisionReason")
    private String decisionReason;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("category")
    private String category;

    /** @deprecated legacy free-form JSON blob; new rows write typed columns instead. */
    @Deprecated
    @JsonProperty("contextData")
    private String contextData;

    @JsonProperty("timestamp")
    private String timestamp;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getCurrentHash() { return currentHash; }
    public void setCurrentHash(String currentHash) { this.currentHash = currentHash; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Deprecated
    public String getContextData() { return contextData; }
    @Deprecated
    public void setContextData(String contextData) { this.contextData = contextData; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
