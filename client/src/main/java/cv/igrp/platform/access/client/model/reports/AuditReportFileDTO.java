package cv.igrp.platform.access.client.model.reports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * An archived report export. Field names and nullability mirror the server DTO
 * {@code cv.igrp.platform.access_management.security_audit.application.dto.AuditReportFileDTO}.
 *
 * <p><strong>{@link #getFilePath()} is the retrieval handle.</strong> No download
 * URL is carried here, because presigned links expire (server default 300s).
 * To open an archived report, pass the path to the files API:
 *
 * <pre>{@code
 * AuditReportFileDTO archived = auditReportsApi.archiveAuditReport(ReportFormat.XLSX, from, to, ...);
 * FileUrlDTO link = filesApi.getPrivateFilesUrl(archived.getFilePath());
 * // link.getUrl() is a short-lived presigned URL to the document
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditReportFileDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("reportType")
    private ReportType reportType;

    @JsonProperty("format")
    private ReportFormat format;

    /** Storage key, e.g. {@code private/audit-reports/<user>/<uuid>_audit-report-2026-07-16.xlsx}. */
    @JsonProperty("filePath")
    private String filePath;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("contentType")
    private String contentType;

    @JsonProperty("sizeBytes")
    private Long sizeBytes;

    /** Number of data rows in the document, excluding the header. */
    @JsonProperty("rowCount")
    private Long rowCount;

    /** JSON snapshot of the filters the report was generated with. */
    @JsonProperty("filters")
    private String filters;

    @JsonProperty("periodStart")
    private Instant periodStart;

    @JsonProperty("periodEnd")
    private Instant periodEnd;

    @JsonProperty("generatedBy")
    private String generatedBy;

    @JsonProperty("generatedAt")
    private Instant generatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public ReportFormat getFormat() {
        return format;
    }

    public void setFormat(ReportFormat format) {
        this.format = format;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Instant periodStart) {
        this.periodStart = periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Instant periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
