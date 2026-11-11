package cv.igrp.framework.auth.core.route;

import cv.igrp.framework.auth.core.audit.Audit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

/**
 * Audits the route filter's access decisions (v3-07): denials and requests made as root.
 * Each event is written through the starter's {@link Audit} in a transaction of its own,
 * so it survives a rollback of the request. Without the audit tables (the service has no
 * PostgreSQL, or {@code igrp.audit.enabled=false}) it goes to the log instead. A failed
 * write is logged as an error, never swallowed silently.
 */
public class AccessAudit {

    static final String DENIED = "access.denied";
    static final String ROOT = "access.root";

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessAudit.class);

    private final Audit audit;
    private final TransactionTemplate ownTransaction;

    /**
     * @param audit          the starter's audit, or {@code null} when it is not configured
     * @param ownTransaction a REQUIRES_NEW template, or {@code null} together with {@code audit}
     */
    public AccessAudit(Audit audit, TransactionTemplate ownTransaction) {
        this.audit = audit;
        this.ownTransaction = ownTransaction;
    }

    public void denied(String method, String path, String key) {
        write(DENIED, method, path, key);
    }

    public void root(String method, String path, String key) {
        write(ROOT, method, path, key);
    }

    private void write(String event, String method, String path, String key) {
        String resource = method + " " + path;
        Map<String, Object> payload = key == null ? Map.of("method", method, "path", path)
                : Map.of("method", method, "path", path, "key", key);
        if (audit == null || !audit.available()) {
            LOGGER.warn("[Access Audit] {} {} {}", event, resource, payload);
            return;
        }
        try {
            ownTransaction.executeWithoutResult(status -> audit.record(event, resource, payload));
        } catch (RuntimeException ex) {
            LOGGER.error("[Access Audit] Could not record {} {} {}", event, resource, payload, ex);
        }
    }
}
