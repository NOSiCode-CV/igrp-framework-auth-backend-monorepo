package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.PageResponse;
import cv.igrp.platform.access.client.model.reports.AccessReportRowDTO;
import cv.igrp.platform.access.client.model.reports.AuditReportFileDTO;
import cv.igrp.platform.access.client.model.reports.AuditReportRowDTO;
import cv.igrp.platform.access.client.model.reports.AuditStatus;
import cv.igrp.platform.access.client.model.reports.ReportFormat;
import cv.igrp.platform.access.client.model.reports.ReportType;
import cv.igrp.platform.access.client.model.reports.SettingsArea;
import cv.igrp.platform.access.client.model.reports.SettingsReportRowDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditReportsApiTest {

    private static final Instant START = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-07-15T23:59:59Z");

    @Mock
    private IApiClient apiClient;

    private AuditReportsApi api;

    @BeforeEach
    void setUp() {
        api = new AuditReportsApi(apiClient);
    }

    // ---------------------------------------------------------------- JSON

    @Test
    @SuppressWarnings("unchecked")
    void getAuditReport_deserialisesRowsAndForwardsFilters() throws ApiException, IOException, InterruptedException {
        String json = """
                {
                  "content": [
                    {"id":"a1","username":"alice","module":"users","accessRole":"ADMIN",
                     "ipAddress":"10.0.0.1","device":"Chrome/Windows","status":"SUCCESS"}
                  ],
                  "totalElements": 1, "totalPages": 1, "size": 20, "number": 0,
                  "first": true, "last": true, "empty": false
                }
                """;
        when(apiClient.invokeAPIRaw(eq("/api/auth/reports/audit"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(json);

        PageResponse<AuditReportRowDTO> page = api.getAuditReport(
                START, END, "alice", "users", "ADMIN", null, null, "SUCCESS", 0, 20, "timestamp,desc");

        assertEquals(1, page.getTotalElements());
        AuditReportRowDTO row = page.getContent().get(0);
        assertEquals("alice", row.getUsername());
        assertEquals("Chrome/Windows", row.getDevice());
        assertEquals(AuditStatus.SUCCESS, row.getStatus());

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(apiClient).invokeAPIRaw(eq("/api/auth/reports/audit"), eq("GET"), captor.capture(), isNull(), isNull());
        Map<String, String> q = captor.getValue();
        assertEquals("2026-07-01T00:00:00Z", q.get("startDate"));
        assertEquals("2026-07-15T23:59:59Z", q.get("endDate"));
        assertEquals("alice", q.get("username"));
        assertEquals("ADMIN", q.get("accessRole"));
        assertEquals("SUCCESS", q.get("status"));
        assertFalse(q.containsKey("operationState"));
        assertFalse(q.containsKey("authorizedBy"));
    }

    @Test
    void getAuditReport_missingStartDate_throwsBadRequest() {
        ApiException ex = assertThrows(ApiException.class, () -> api.getAuditReport(
                null, END, null, null, null, null, null, null, 0, 20, null));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(apiClient);
    }

    @Test
    void getAuditReport_missingEndDate_throwsBadRequest() {
        ApiException ex = assertThrows(ApiException.class, () -> api.getAuditReport(
                START, null, null, null, null, null, null, null, 0, 20, null));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(apiClient);
    }

    @Test
    void getAccessReport_deserialisesRows() throws ApiException, IOException, InterruptedException {
        String json = """
                {"content":[{"username":"bob","role":"VIEWER","module":"reports",
                 "action":"EXPORT","ipAddress":"10.0.0.2","status":"ACCESS_DENIED"}],
                 "totalElements":1,"empty":false}
                """;
        when(apiClient.invokeAPIRaw(eq("/api/auth/reports/access"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(json);

        PageResponse<AccessReportRowDTO> page = api.getAccessReport(
                START, END, "bob", "VIEWER", "reports", "EXPORT", "ACCESS_DENIED", 0, 20, null);

        AccessReportRowDTO row = page.getContent().get(0);
        assertEquals("bob", row.getUsername());
        assertEquals("EXPORT", row.getAction());
        assertEquals(AuditStatus.ACCESS_DENIED, row.getStatus());
    }

    @Test
    void getSettingsReport_deserialisesEnums() throws ApiException, IOException, InterruptedException {
        String json = """
                {"content":[{"performedBy":"carol","area":"APPLICATIONS","entityType":"APPLICATION",
                 "operation":"CREATE","entityName":"Payments","status":"SUCCESS"}],
                 "totalElements":1,"empty":false}
                """;
        when(apiClient.invokeAPIRaw(eq("/api/auth/reports/settings"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(json);

        PageResponse<SettingsReportRowDTO> page = api.getSettingsReport(
                START, END, "carol", "APPLICATIONS", "APPLICATION", "CREATE", "Payments", 0, 20, null);

        SettingsReportRowDTO row = page.getContent().get(0);
        assertEquals("carol", row.getPerformedBy());
        assertEquals(SettingsArea.APPLICATIONS, row.getArea());
        assertEquals("Payments", row.getEntityName());
    }

    // -------------------------------------------------------------- Exports

    @Test
    @SuppressWarnings("unchecked")
    void getAuditReportXlsx_streamsFromExportEndpoint() throws ApiException, IOException {
        byte[] payload = "PK-xlsx-bytes".getBytes(StandardCharsets.UTF_8);
        when(apiClient.invokeAPIStream(eq("/api/auth/reports/audit.xlsx"), eq("GET"), anyMap(), isNull()))
                .thenReturn(new ByteArrayInputStream(payload));

        try (InputStream in = api.getAuditReportXlsx(
                START, END, "alice", null, null, null, null, null)) {
            assertArrayEquals(payload, in.readAllBytes());
        }

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(apiClient).invokeAPIStream(eq("/api/auth/reports/audit.xlsx"), eq("GET"), captor.capture(), isNull());
        Map<String, String> q = captor.getValue();
        assertEquals("2026-07-01T00:00:00Z", q.get("startDate"));
        assertEquals("alice", q.get("username"));
        assertFalse(q.containsKey("page"), "exports must not carry pagination");
        assertFalse(q.containsKey("size"));
    }

    @Test
    void getSettingsReportPdf_hitsPdfEndpoint() throws ApiException, IOException {
        byte[] payload = "%PDF-1.7".getBytes(StandardCharsets.UTF_8);
        when(apiClient.invokeAPIStream(eq("/api/auth/reports/settings.pdf"), eq("GET"), anyMap(), isNull()))
                .thenReturn(new ByteArrayInputStream(payload));

        try (InputStream in = api.getSettingsReportPdf(
                START, END, null, "USERS", null, null, null)) {
            assertArrayEquals(payload, in.readAllBytes());
        }
        verify(apiClient).invokeAPIStream(eq("/api/auth/reports/settings.pdf"), eq("GET"), anyMap(), isNull());
    }

    @Test
    void export_missingDate_throwsBeforeCall() {
        assertThrows(ApiException.class, () -> api.getAccessReportPdf(
                null, END, null, null, null, null, null));
        verifyNoInteractions(apiClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAuditReportCsv_streamsFromCsvEndpointWithoutPaging() throws ApiException, IOException {
        byte[] payload = "﻿ID,Start Date\r\n".getBytes(StandardCharsets.UTF_8);
        when(apiClient.invokeAPIStream(eq("/api/auth/reports/audit.csv"), eq("GET"), anyMap(), isNull()))
                .thenReturn(new ByteArrayInputStream(payload));

        try (InputStream in = api.getAuditReportCsv(
                START, END, "alice", null, null, null, null, null)) {
            assertArrayEquals(payload, in.readAllBytes());
        }

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(apiClient).invokeAPIStream(eq("/api/auth/reports/audit.csv"), eq("GET"), captor.capture(), isNull());
        Map<String, String> q = captor.getValue();
        assertEquals("alice", q.get("username"));
        assertFalse(q.containsKey("page"), "exports must not carry pagination");
    }

    @Test
    void accessAndSettingsCsv_hitTheirOwnEndpoints() throws ApiException, IOException {
        when(apiClient.invokeAPIStream(anyString(), eq("GET"), anyMap(), isNull()))
                .thenReturn(new ByteArrayInputStream(new byte[0]));

        api.getAccessReportCsv(START, END, null, null, null, null, null).close();
        api.getSettingsReportCsv(START, END, null, null, null, null, null).close();

        verify(apiClient).invokeAPIStream(eq("/api/auth/reports/access.csv"), eq("GET"), anyMap(), isNull());
        verify(apiClient).invokeAPIStream(eq("/api/auth/reports/settings.csv"), eq("GET"), anyMap(), isNull());
    }

    // -------------------------------------------------------------- Archive

    @Test
    @SuppressWarnings("unchecked")
    void archiveAuditReport_postsFormatWithFiltersAndReturnsTheRecord() throws ApiException {
        AuditReportFileDTO stored = new AuditReportFileDTO();
        stored.setId("f1");
        stored.setReportType(ReportType.AUDIT);
        stored.setFormat(ReportFormat.XLSX);
        stored.setFilePath("private/audit-reports/alice/uuid_audit-report-2026-07-16.xlsx");
        stored.setRowCount(42L);
        when(apiClient.invokeAPI(eq("/api/auth/reports/audit/archive"), eq("POST"), anyMap(), isNull(), isNull(),
                eq(AuditReportFileDTO.class))).thenReturn(stored);

        AuditReportFileDTO result = api.archiveAuditReport(
                ReportFormat.XLSX, START, END, "alice", null, null, null, null, null);

        assertEquals("f1", result.getId());
        assertEquals(ReportFormat.XLSX, result.getFormat());
        // filePath is the handle the caller feeds to FilesApi.getPrivateFilesUrl(...)
        assertTrue(result.getFilePath().startsWith("private/audit-reports/"));
        assertEquals(42L, result.getRowCount());

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(apiClient).invokeAPI(eq("/api/auth/reports/audit/archive"), eq("POST"), captor.capture(),
                isNull(), isNull(), eq(AuditReportFileDTO.class));
        Map<String, String> q = captor.getValue();
        assertEquals("XLSX", q.get("format"));
        assertEquals("2026-07-01T00:00:00Z", q.get("startDate"));
        assertEquals("alice", q.get("username"));
        assertFalse(q.containsKey("page"), "archive must not carry pagination");
    }

    @Test
    void archiveAccessAndSettings_hitTheirOwnEndpoints() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("POST"), anyMap(), isNull(), isNull(),
                eq(AuditReportFileDTO.class))).thenReturn(new AuditReportFileDTO());

        api.archiveAccessReport(ReportFormat.CSV, START, END, null, null, null, null, null);
        api.archiveSettingsReport(ReportFormat.PDF, START, END, null, null, null, null, null);

        verify(apiClient).invokeAPI(eq("/api/auth/reports/access/archive"), eq("POST"), anyMap(), isNull(),
                isNull(), eq(AuditReportFileDTO.class));
        verify(apiClient).invokeAPI(eq("/api/auth/reports/settings/archive"), eq("POST"), anyMap(), isNull(),
                isNull(), eq(AuditReportFileDTO.class));
    }

    @Test
    void archive_missingFormat_throwsBeforeCall() {
        assertThrows(ApiException.class, () -> api.archiveAuditReport(
                null, START, END, null, null, null, null, null, null));
        verifyNoInteractions(apiClient);
    }

    @Test
    void archive_missingDate_throwsBeforeCall() {
        assertThrows(ApiException.class, () -> api.archiveSettingsReport(
                ReportFormat.CSV, null, END, null, null, null, null, null));
        verifyNoInteractions(apiClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listArchives_deserialisesRecordsAndForwardsFilters()
            throws ApiException, IOException, InterruptedException {
        String json = """
                {
                  "content": [
                    {"id":"f1","reportType":"SETTINGS","format":"CSV",
                     "filePath":"private/audit-reports/alice/uuid_settings-report-2026-07-16.csv",
                     "fileName":"settings-report-2026-07-16.csv","sizeBytes":128,"rowCount":9,
                     "generatedBy":"alice","generatedAt":"2026-07-16T10:00:00Z"}
                  ],
                  "totalElements": 1, "totalPages": 1, "size": 20, "number": 0,
                  "first": true, "last": true, "empty": false
                }
                """;
        when(apiClient.invokeAPIRaw(eq("/api/auth/reports/archives"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(json);

        PageResponse<AuditReportFileDTO> page = api.listArchives("SETTINGS", "CSV", "alice", 0, 20, null);

        assertEquals(1, page.getContent().size());
        AuditReportFileDTO row = page.getContent().get(0);
        assertEquals(ReportType.SETTINGS, row.getReportType());
        assertEquals(ReportFormat.CSV, row.getFormat());
        assertTrue(row.getFilePath().startsWith("private/audit-reports/"));
        assertEquals(Instant.parse("2026-07-16T10:00:00Z"), row.getGeneratedAt());

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(apiClient).invokeAPIRaw(eq("/api/auth/reports/archives"), eq("GET"), captor.capture(),
                isNull(), isNull());
        Map<String, String> q = captor.getValue();
        assertEquals("SETTINGS", q.get("reportType"));
        assertEquals("CSV", q.get("format"));
        assertEquals("alice", q.get("generatedBy"));
        assertEquals("0", q.get("page"));
        // listArchives has no date bounds — unlike the reports, they are not required
        assertFalse(q.containsKey("startDate"));
    }
}
