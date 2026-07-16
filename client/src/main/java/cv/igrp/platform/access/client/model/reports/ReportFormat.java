package cv.igrp.platform.access.client.model.reports;

/**
 * Export format of a report — both the download variants and the format an
 * archive request is generated in.
 *
 * <p>Mirrors the server enum
 * {@code cv.igrp.platform.access_management.security_audit.domain.enums.ReportFormat}.
 */
public enum ReportFormat {

    PDF("pdf", "application/pdf"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    /** Charset is explicit: the payload is UTF-8 (with BOM), and CSV has no in-band encoding declaration. */
    CSV("csv", "text/csv;charset=UTF-8");

    private final String extension;
    private final String contentType;

    ReportFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    /** File extension without the dot, e.g. {@code "xlsx"}. */
    public String extension() {
        return extension;
    }

    /** MIME type the server responds with for this format. */
    public String contentType() {
        return contentType;
    }
}
