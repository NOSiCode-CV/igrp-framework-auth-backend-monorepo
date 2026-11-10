package cv.igrp.framework.auth.core.audit.revision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * The Envers revision of every iGRP service (v3-24): Hibernate's default
 * {@code revinfo} ({@code rev}, {@code revtstmp}, sequence {@code revinfo_seq}
 * by 50) plus who made the change and the request's trace id, so the change
 * history joins the audit event and the trace.
 *
 * <p>Services add the two columns in their own migration:
 * <pre>{@code
 * ALTER TABLE revinfo ADD COLUMN username VARCHAR(255);
 * ALTER TABLE revinfo ADD COLUMN trace_id VARCHAR(64);
 * }</pre>
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(IgrpRevisionListener.class)
public class IgrpRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "igrp_revinfo_seq")
    @SequenceGenerator(name = "igrp_revinfo_seq", sequenceName = "revinfo_seq", allocationSize = 50)
    @RevisionNumber
    @Column(name = "rev")
    private int rev;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private long timestamp;

    @Column(name = "username")
    private String username;

    /** The request's correlation id: the OTel trace id, or the {@code X-Correlation-ID} fallback. */
    @Column(name = "trace_id", length = 64)
    private String traceId;

    public int getRev() {
        return rev;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
