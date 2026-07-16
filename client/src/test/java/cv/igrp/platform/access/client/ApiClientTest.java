package cv.igrp.platform.access.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Query-string construction. Audit filters carry user-supplied text
 * ({@code generatedBy} is an email, {@code entityName} is free text), so values
 * must be percent-encoded — unencoded they either corrupt the query or make
 * {@code URI.create} throw.
 */
class ApiClientTest {

    private ApiClient apiClient;

    @BeforeEach
    void setUp() {
        apiClient = new ApiClient();
        apiClient.setBaseUrl("http://localhost:8081");
    }

    @Test
    void buildUri_withNoParams_returnsBarePath() {
        assertEquals("http://localhost:8081/api/auth/reports/archives",
                apiClient.buildUri("/api/auth/reports/archives", null));
        assertEquals("http://localhost:8081/api/auth/reports/archives",
                apiClient.buildUri("/api/auth/reports/archives", Map.of()));
    }

    @Test
    void buildUri_encodesSpacesSoTheUriIsParseable() {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("entityName", "my application name");

        String uri = apiClient.buildUri("/api/auth/reports/settings", q);

        assertEquals("http://localhost:8081/api/auth/reports/settings?entityName=my+application+name", uri);
        // Unencoded, this threw IllegalArgumentException.
        assertDoesNotThrow(() -> URI.create(uri));
    }

    @Test
    void buildUri_encodesReservedCharactersThatWouldCorruptTheQuery() {
        Map<String, String> q = new LinkedHashMap<>();
        // A bare & would start a new parameter; a bare + would arrive as a space.
        q.put("entityName", "A&B+C");
        q.put("generatedBy", "marcelo.monteiro@nosi.cv");

        String uri = apiClient.buildUri("/api/auth/reports/archives", q);

        assertEquals("http://localhost:8081/api/auth/reports/archives"
                + "?entityName=A%26B%2BC&generatedBy=marcelo.monteiro%40nosi.cv", uri);
        assertDoesNotThrow(() -> URI.create(uri));
    }

    @Test
    void buildUri_preservesParameterOrderAndSeparators() {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("startDate", "2026-07-01T00:00:00Z");
        q.put("endDate", "2026-07-15T23:59:59Z");
        q.put("format", "XLSX");

        String uri = apiClient.buildUri("/api/auth/reports/audit/archive", q);

        // ':' is encoded by URLEncoder; servers decode it back for @RequestParam.
        assertEquals("http://localhost:8081/api/auth/reports/audit/archive"
                + "?startDate=2026-07-01T00%3A00%3A00Z"
                + "&endDate=2026-07-15T23%3A59%3A59Z"
                + "&format=XLSX", uri);
        assertDoesNotThrow(() -> URI.create(uri));
    }
}
