package cv.igrp.platform.access.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.JsonUtil;
import cv.igrp.platform.access.client.model.AuditChainValidationDTO;
import cv.igrp.platform.access.client.model.AuditLogDTO;
import cv.igrp.platform.access.client.model.AuditPurgeResultDTO;
import cv.igrp.platform.access.client.model.PageResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client for the unified, tamper-evident audit log ({@code /api/auth/audit}).
 *
 * <p>Replaces the legacy {@code AuthAuditApi}: the three former GETs
 * ({@code /}, {@code /{id}}, {@code /user/{userId}}) collapsed into a single
 * filtered, paginated {@link #list} (R1.3 / N6), and the tamper-evidence
 * controls were added — {@link #validate()} verifies the hash chain on demand,
 * {@link #purge()} clears the trail (gated separately by
 * {@code igrp.audit.purge}). The list and validate endpoints are gated by
 * {@code igrp.audit.view}.
 */
public class AuditLogClientApi {

    private IApiClient apiClient;

    public AuditLogClientApi() {
        this(new ApiClient());
    }

    public AuditLogClientApi(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public IApiClient getIApiClient() {
        return apiClient;
    }

    public void setIApiClient(IApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Paginated, filtered view of the unified audit log
     * ({@code GET /api/auth/audit}). All filters are optional. {@code startDate}
     * / {@code endDate} are ISO-8601 local date-times (e.g.
     * {@code "2026-07-15T00:00:00"}); {@code sort} follows Spring's
     * {@code field,direction} convention.
     */
    public PageResponse<AuditLogDTO> list(
            String userId, String username, String eventType, String category, String ipAddress,
            String startDate, String endDate,
            Integer page, Integer size, String sort)
            throws ApiException, IOException, InterruptedException {
        Map<String, String> q = new LinkedHashMap<>();
        putIfPresent(q, "userId", userId);
        putIfPresent(q, "username", username);
        putIfPresent(q, "eventType", eventType);
        putIfPresent(q, "category", category);
        putIfPresent(q, "ipAddress", ipAddress);
        putIfPresent(q, "startDate", startDate);
        putIfPresent(q, "endDate", endDate);
        if (page != null) q.put("page", page.toString());
        if (size != null) q.put("size", size.toString());
        putIfPresent(q, "sort", sort);

        String raw = apiClient.invokeAPIRaw(
                "/api/auth/audit", "GET", q.isEmpty() ? null : q, null, null);
        return JsonUtil.deserialize(raw, new TypeReference<PageResponse<AuditLogDTO>>() {});
    }

    /**
     * Verify the tamper-evident hash chain on demand
     * ({@code POST /api/auth/audit/validate}).
     */
    public AuditChainValidationDTO validate() throws ApiException {
        return apiClient.invokeAPI(
                "/api/auth/audit/validate", "POST", null, null, null, AuditChainValidationDTO.class);
    }

    /**
     * Purge every audit row except the GENESIS anchor and restart the chain
     * ({@code DELETE /api/auth/audit/purge}). Gated by {@code igrp.audit.purge}.
     */
    public AuditPurgeResultDTO purge() throws ApiException {
        return apiClient.invokeAPI(
                "/api/auth/audit/purge", "DELETE", null, null, null, AuditPurgeResultDTO.class);
    }

    private static void putIfPresent(Map<String, String> q, String key, String value) {
        if (value != null && !value.isBlank()) {
            q.put(key, value);
        }
    }
}
