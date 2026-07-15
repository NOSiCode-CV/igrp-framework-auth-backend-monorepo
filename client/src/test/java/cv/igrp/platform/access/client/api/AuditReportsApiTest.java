package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.PageResponse;
import cv.igrp.platform.access.client.model.reports.AccessReportRowDTO;
import cv.igrp.platform.access.client.model.reports.AuditReportRowDTO;
import cv.igrp.platform.access.client.model.reports.AuditStatus;
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
}
