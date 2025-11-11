package cv.igrp.framework.auth.core.authorization.model;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents a generic request for checking a permission, abstract enough
 * to be compatible with multiple backends like Redis, Permify, etc.
 */
public class PermissionCheckRequest implements Serializable {

    /**
     * The resource identifier (e.g., "document:123" or "vehicle:ABC123").
     */
    private String resource;

    /**
     * The action to be performed on the resource (e.g., "read", "update").
     */
    private String action;

    /**
     * The subject (user or service) requesting the permission.
     * Example: "user:john" or "service:billing-service"
     */
    private String subject;

    /**
     * Optional context map for additional provider-specific data.
     */
    private Map<String, Object> context;

    public PermissionCheckRequest() {
    }

    public PermissionCheckRequest(String resource, String action, String subject) {
        this.resource = resource;
        this.action = action;
        this.subject = subject;
    }

    public PermissionCheckRequest(String resource, String action, String subject, Map<String, Object> context) {
        this.resource = resource;
        this.action = action;
        this.subject = subject;
        this.context = context;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}
