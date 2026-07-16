package cv.igrp.platform.access.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.JsonUtil;
import cv.igrp.platform.access.client.model.PageResponse;
import cv.igrp.platform.access.client.model.reports.AccessReportRowDTO;
import cv.igrp.platform.access.client.model.reports.AuditReportFileDTO;
import cv.igrp.platform.access.client.model.reports.AuditReportRowDTO;
import cv.igrp.platform.access.client.model.reports.ReportFormat;
import cv.igrp.platform.access.client.model.reports.SettingsReportRowDTO;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client for the reporting surface over the unified audit log
 * ({@code /api/auth/reports}). Exposes the three paginated JSON reports (Audit /
 * Access / Settings), their PDF, Excel and CSV export variants, and the report
 * archive.
 *
 * <p>All endpoints are gated server-side by the single {@code igrp.audit.view}
 * permission. {@code startDate} and {@code endDate} are required on every call;
 * a missing bound yields a 400 ({@link ApiException} with code 400). Export
 * methods take the same filters as their JSON siblings, minus pagination, and
 * return the raw response {@link InputStream} — the caller owns and must close
 * it.
 *
 * <p>Two ways to get a document:
 * <ul>
 *   <li><strong>Download</strong> ({@code get*Pdf/Xlsx/Csv}) — streams the bytes
 *       back immediately.</li>
 *   <li><strong>Archive</strong> ({@code archive*}) — the server stores the
 *       document and returns an {@link AuditReportFileDTO}. Take its
 *       {@code filePath} and resolve it through
 *       {@link FilesApi#getPrivateFilesUrl(String)} for a short-lived presigned
 *       link. Nothing is downloaded by the archive call itself.</li>
 * </ul>
 */
public class AuditReportsApi {

    private IApiClient apiClient;

    public AuditReportsApi() {
        this(new ApiClient());
    }

    public AuditReportsApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // ------------------------------------------------------------------ JSON

    /** Audit Report — {@code GET /api/auth/reports/audit}. */
    public PageResponse<AuditReportRowDTO> getAuditReport(
            Instant startDate, Instant endDate,
            String username, String module, String accessRole, String operationState,
            String authorizedBy, String status,
            Integer page, Integer size, String sort)
            throws ApiException, IOException, InterruptedException {
        Map<String, String> q = requireDates(startDate, endDate);
        putIfPresent(q, "username", username);
        putIfPresent(q, "module", module);
        putIfPresent(q, "accessRole", accessRole);
        putIfPresent(q, "operationState", operationState);
        putIfPresent(q, "authorizedBy", authorizedBy);
        putIfPresent(q, "status", status);
        putPaging(q, page, size, sort);

        String raw = apiClient.invokeAPIRaw("/api/auth/reports/audit", "GET", q, null, null);
        return JsonUtil.deserialize(raw, new TypeReference<PageResponse<AuditReportRowDTO>>() {});
    }

    /** Access Report — {@code GET /api/auth/reports/access}. */
    public PageResponse<AccessReportRowDTO> getAccessReport(
            Instant startDate, Instant endDate,
            String username, String role, String module, String action, String status,
            Integer page, Integer size, String sort)
            throws ApiException, IOException, InterruptedException {
        Map<String, String> q = requireDates(startDate, endDate);
        putIfPresent(q, "username", username);
        putIfPresent(q, "role", role);
        putIfPresent(q, "module", module);
        putIfPresent(q, "action", action);
        putIfPresent(q, "status", status);
        putPaging(q, page, size, sort);

        String raw = apiClient.invokeAPIRaw("/api/auth/reports/access", "GET", q, null, null);
        return JsonUtil.deserialize(raw, new TypeReference<PageResponse<AccessReportRowDTO>>() {});
    }

    /** Settings Report — {@code GET /api/auth/reports/settings}. */
    public PageResponse<SettingsReportRowDTO> getSettingsReport(
            Instant startDate, Instant endDate,
            String performedBy, String area, String entityType, String operation, String entityName,
            Integer page, Integer size, String sort)
            throws ApiException, IOException, InterruptedException {
        Map<String, String> q = requireDates(startDate, endDate);
        putIfPresent(q, "performedBy", performedBy);
        putIfPresent(q, "area", area);
        putIfPresent(q, "entityType", entityType);
        putIfPresent(q, "operation", operation);
        putIfPresent(q, "entityName", entityName);
        putPaging(q, page, size, sort);

        String raw = apiClient.invokeAPIRaw("/api/auth/reports/settings", "GET", q, null, null);
        return JsonUtil.deserialize(raw, new TypeReference<PageResponse<SettingsReportRowDTO>>() {});
    }

    // --------------------------------------------------------------- Exports

    /** Audit Report as XLSX — {@code GET /api/auth/reports/audit.xlsx}. */
    public InputStream getAuditReportXlsx(
            Instant startDate, Instant endDate,
            String username, String module, String accessRole, String operationState,
            String authorizedBy, String status) throws ApiException {
        return apiClient.invokeAPIStream("/api/auth/reports/audit.xlsx", "GET",
                auditFilters(startDate, endDate, username, module, accessRole, operationState, authorizedBy, status),
                null);
    }

    /** Audit Report as PDF — {@code GET /api/auth/reports/audit.pdf}. */
    public InputStream getAuditReportPdf(
            Instant startDate, Instant endDate,
            String username, String module, String accessRole, String operationState,
            String authorizedBy, String status) throws ApiException {
        return apiClient.invokeAPIStream("/api/auth/reports/audit.pdf", "GET",
                auditFilters(startDate, endDate, username, module, accessRole, operationState, authorizedBy, status),
                null);
    }

    /** Access Report as XLSX — {@code GET /api/auth/reports/access.xlsx}. */
    public InputStream getAccessReportXlsx(
            Instant startDate, Instant endDate,
            String username, String role, String module, String action, String status) throws ApiException {
        return apiClient.invokeAPIStream("/api/auth/reports/access.xlsx", "GET",
                accessFilters(startDate, endDate, username, role, module, action, status), null);
    }

    /** Access Report as PDF — {@code GET /api/auth/reports/access.pdf}. */
    public InputStream getAccessReportPdf(
            Instant startDate, Instant endDate,
            String username, String role, String module, String action, String status) throws ApiException {
        return apiClient.invokeAPIStream("/api/auth/reports/access.pdf", "GET",
                accessFilters(startDate, endDate, username, role, module, action, status), null);
    }

    /** Settings Report as XLSX — {@code GET /api/auth/reports/settings.xlsx}. */
    public InputStream getSettingsReportXlsx(
            Instant startDate, Instant endDate,
            String performedBy, String area, String entityType, String operation, String entityName)
            throws ApiException {
        return apiClient.invokeAPIStream("/api/auth/reports/settings.xlsx", "GET",
                settingsFilters(startDate, endDate, performedBy, area, entityType, operation, entityName), null);
    }

    /** Settings Report as PDF — {@code GET /api/auth/reports/settings.pdf}. */
    public InputStream getSettingsReportPdf(
            Instant startDate, Instant endDate,
            String performedBy, String area, String entityType, String operation, String entityName)
            throws ApiException {
        return apiClient.invokeAPIStream("/api/auth/reports/settings.pdf", "GET",
                settingsFilters(startDate, endDate, performedBy, area, entityType, operation, entityName), null);
    }

    /** Audit Report as CSV — {@code GET /api/auth/reports/audit.csv}. */
    public InputStream getAuditReportCsv(
            Instant startDate, Instant endDate,
            String username, String module, String accessRole, String operationState,
            String authorizedBy, String status) throws ApiException {
        return apiClient.invokeAPIStream("/api/auth/reports/audit.csv", "GET",
                auditFilters(startDate, endDate, username, module, accessRole, operationState, authorizedBy, status),
                null);
    }

    /** Access Report as CSV — {@code GET /api/auth/reports/access.csv}. */
    public InputStream getAccessReportCsv(
            Instant startDate, Instant endDate,
            String username, String role, String module, String action, String status) throws ApiException {
        return apiClient.invokeAPIStream("/api/auth/reports/access.csv", "GET",
                accessFilters(startDate, endDate, username, role, module, action, status), null);
    }

    /** Settings Report as CSV — {@code GET /api/auth/reports/settings.csv}. */
    public InputStream getSettingsReportCsv(
            Instant startDate, Instant endDate,
            String performedBy, String area, String entityType, String operation, String entityName)
            throws ApiException {
        return apiClient.invokeAPIStream("/api/auth/reports/settings.csv", "GET",
                settingsFilters(startDate, endDate, performedBy, area, entityType, operation, entityName), null);
    }

    // --------------------------------------------------------------- Archive

    /**
     * Archive the Audit Report — {@code POST /api/auth/reports/audit/archive}.
     * The server generates the document in {@code format}, stores it, and returns
     * the record. Resolve {@link AuditReportFileDTO#getFilePath()} through
     * {@link FilesApi#getPrivateFilesUrl(String)} to download it.
     */
    public AuditReportFileDTO archiveAuditReport(
            ReportFormat format, Instant startDate, Instant endDate,
            String username, String module, String accessRole, String operationState,
            String authorizedBy, String status)
            throws ApiException {
        Map<String, String> q = auditFilters(
                startDate, endDate, username, module, accessRole, operationState, authorizedBy, status);
        putFormat(q, format);
        return apiClient.invokeAPI("/api/auth/reports/audit/archive", "POST", q, null, null,
                AuditReportFileDTO.class);
    }

    /** Archive the Access Report — {@code POST /api/auth/reports/access/archive}. */
    public AuditReportFileDTO archiveAccessReport(
            ReportFormat format, Instant startDate, Instant endDate,
            String username, String role, String module, String action, String status)
            throws ApiException {
        Map<String, String> q = accessFilters(startDate, endDate, username, role, module, action, status);
        putFormat(q, format);
        return apiClient.invokeAPI("/api/auth/reports/access/archive", "POST", q, null, null,
                AuditReportFileDTO.class);
    }

    /** Archive the Settings Report — {@code POST /api/auth/reports/settings/archive}. */
    public AuditReportFileDTO archiveSettingsReport(
            ReportFormat format, Instant startDate, Instant endDate,
            String performedBy, String area, String entityType, String operation, String entityName)
            throws ApiException {
        Map<String, String> q = settingsFilters(
                startDate, endDate, performedBy, area, entityType, operation, entityName);
        putFormat(q, format);
        return apiClient.invokeAPI("/api/auth/reports/settings/archive", "POST", q, null, null,
                AuditReportFileDTO.class);
    }

    /**
     * List archived report exports, newest first —
     * {@code GET /api/auth/reports/archives}. All filters are optional.
     */
    public PageResponse<AuditReportFileDTO> listArchives(
            String reportType, String format, String generatedBy,
            Integer page, Integer size, String sort)
            throws ApiException, IOException, InterruptedException {
        Map<String, String> q = new LinkedHashMap<>();
        putIfPresent(q, "reportType", reportType);
        putIfPresent(q, "format", format);
        putIfPresent(q, "generatedBy", generatedBy);
        putPaging(q, page, size, sort);

        String raw = apiClient.invokeAPIRaw("/api/auth/reports/archives", "GET", q, null, null);
        return JsonUtil.deserialize(raw, new TypeReference<PageResponse<AuditReportFileDTO>>() {});
    }

    // --------------------------------------------------------------- helpers

    /** {@code format} is required by the server; fail fast rather than 400 on the wire. */
    private static void putFormat(Map<String, String> q, ReportFormat format) throws ApiException {
        if (format == null) {
            throw new ApiException(400, "Missing the required parameter 'format'");
        }
        q.put("format", format.name());
    }

    private Map<String, String> auditFilters(
            Instant startDate, Instant endDate,
            String username, String module, String accessRole, String operationState,
            String authorizedBy, String status) throws ApiException {
        Map<String, String> q = requireDates(startDate, endDate);
        putIfPresent(q, "username", username);
        putIfPresent(q, "module", module);
        putIfPresent(q, "accessRole", accessRole);
        putIfPresent(q, "operationState", operationState);
        putIfPresent(q, "authorizedBy", authorizedBy);
        putIfPresent(q, "status", status);
        return q;
    }

    private Map<String, String> accessFilters(
            Instant startDate, Instant endDate,
            String username, String role, String module, String action, String status) throws ApiException {
        Map<String, String> q = requireDates(startDate, endDate);
        putIfPresent(q, "username", username);
        putIfPresent(q, "role", role);
        putIfPresent(q, "module", module);
        putIfPresent(q, "action", action);
        putIfPresent(q, "status", status);
        return q;
    }

    private Map<String, String> settingsFilters(
            Instant startDate, Instant endDate,
            String performedBy, String area, String entityType, String operation, String entityName) throws ApiException {
        Map<String, String> q = requireDates(startDate, endDate);
        putIfPresent(q, "performedBy", performedBy);
        putIfPresent(q, "area", area);
        putIfPresent(q, "entityType", entityType);
        putIfPresent(q, "operation", operation);
        putIfPresent(q, "entityName", entityName);
        return q;
    }

    /**
     * Seed the query map with the two required date bounds, formatted as
     * ISO-8601 instants (what the server's {@code @DateTimeFormat(ISO.DATE_TIME)}
     * expects). Both are mandatory — a null bound fails fast with a 400-style
     * {@link ApiException} before any network call.
     */
    private static Map<String, String> requireDates(Instant startDate, Instant endDate) throws ApiException {
        if (startDate == null) {
            throw new ApiException(400, "Missing the required parameter 'startDate'");
        }
        if (endDate == null) {
            throw new ApiException(400, "Missing the required parameter 'endDate'");
        }
        Map<String, String> q = new LinkedHashMap<>();
        q.put("startDate", startDate.toString());
        q.put("endDate", endDate.toString());
        return q;
    }

    private static void putIfPresent(Map<String, String> q, String key, String value) {
        if (value != null && !value.isBlank()) {
            q.put(key, value);
        }
    }

    private static void putPaging(Map<String, String> q, Integer page, Integer size, String sort) {
        if (page != null) q.put("page", page.toString());
        if (size != null) q.put("size", size.toString());
        if (sort != null && !sort.isBlank()) q.put("sort", sort);
    }
}
