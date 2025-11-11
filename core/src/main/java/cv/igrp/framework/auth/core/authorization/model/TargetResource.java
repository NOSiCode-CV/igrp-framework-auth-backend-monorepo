package cv.igrp.framework.auth.core.authorization.model;

/**
 * Represents a **Resource** in the authorization system.
 * A Resource is an {@link Entity} that can be acted upon or has permissions associated with it.
 * Examples include "document:report_2024", "application:payroll", "folder:shared_docs".
 *
 * @see Entity
 */
public class TargetResource extends Entity {

    /**
     * Default constructor for Resource.
     */
    public TargetResource() {
    }

    /**
     * Constructs a new Resource with a specified type and identifier.
     *
     * @param type The categorization or kind of the resource (e.g., "document", "application", "organization").
     * @param id   The unique identifier of the specific resource instance (e.g., "report_2024", "payroll_app_id", "hr_org_id").
     */
    public TargetResource(String type, String id) {
        super(type, id);
    }

}
