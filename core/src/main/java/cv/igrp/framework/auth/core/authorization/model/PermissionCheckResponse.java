package cv.igrp.framework.auth.core.authorization.model;

import java.io.Serializable;
import java.util.List;

/**
 * Represents the outcome of a permission check, including decision details,
 * roles involved, cache status, and timing information.
 */
public class PermissionCheckResponse implements Serializable {

    /**
     * Whether the permission is granted (true) or denied (false).
     */
    private boolean allowed;

    /**
     * Roles that contributed to the decision, if any.
     */
    private List<String> viaRoles;

    /**
     * Whether the result came from cache (true) or was freshly computed (false).
     */
    private boolean cacheHit;

    /**
     * Time in milliseconds it took to resolve the permission check.
     */
    private long resolutionTimeMs;

    /**
     * Optional explanation for the decision.
     */
    private String reason;

    /**
     * Default constructor for {@code PermissionCheckResponse}.
     */
    public PermissionCheckResponse() {
    }

    public PermissionCheckResponse(boolean allowed, List<String> viaRoles, boolean cacheHit, long resolutionTimeMs, String reason) {
        this.allowed = allowed;
        this.viaRoles = viaRoles;
        this.cacheHit = cacheHit;
        this.resolutionTimeMs = resolutionTimeMs;
        this.reason = reason;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public List<String> getViaRoles() {
        return viaRoles;
    }

    public void setViaRoles(List<String> viaRoles) {
        this.viaRoles = viaRoles;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public long getResolutionTimeMs() {
        return resolutionTimeMs;
    }

    public void setResolutionTimeMs(long resolutionTimeMs) {
        this.resolutionTimeMs = resolutionTimeMs;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
