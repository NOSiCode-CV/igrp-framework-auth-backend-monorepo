package cv.igrp.platform.access.client.model.reports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * One row of the Settings (Configuration) Report
 * ({@code GET /api/auth/reports/settings}). Field names mirror the server DTO
 * {@code cv.igrp.platform.access_management.security_audit.application.dto.SettingsReportRowDTO}.
 * {@code relatedEntity}, {@code previousValue} and {@code newValue} are nullable.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettingsReportRowDTO {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("performedBy")
    private String performedBy;

    @JsonProperty("area")
    private SettingsArea area;

    @JsonProperty("entityType")
    private SettingsEntityType entityType;

    @JsonProperty("operation")
    private SettingsOperation operation;

    @JsonProperty("entityName")
    private String entityName;

    @JsonProperty("relatedEntity")
    private String relatedEntity;

    @JsonProperty("previousValue")
    private String previousValue;

    @JsonProperty("newValue")
    private String newValue;

    @JsonProperty("ipAddress")
    private String ipAddress;

    @JsonProperty("status")
    private AuditStatus status;

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public SettingsArea getArea() { return area; }
    public void setArea(SettingsArea area) { this.area = area; }

    public SettingsEntityType getEntityType() { return entityType; }
    public void setEntityType(SettingsEntityType entityType) { this.entityType = entityType; }

    public SettingsOperation getOperation() { return operation; }
    public void setOperation(SettingsOperation operation) { this.operation = operation; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getRelatedEntity() { return relatedEntity; }
    public void setRelatedEntity(String relatedEntity) { this.relatedEntity = relatedEntity; }

    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public AuditStatus getStatus() { return status; }
    public void setStatus(AuditStatus status) { this.status = status; }
}
