package cv.igrp.framework.auth.core.authorization.model;

/**
 * Represents a relationship tuple in a Zanzibar-like system:
 * resource#relation@subject
 * e.g., "document:doc1#viewer@user:alice"
 * or "folder:f1#reader@group:finance#member" (subject set)
 */
public class RelationshipTuple {

    private TargetResource targetResource;
    private String relation;
    private Subject subject;

    /**
     * Constructs a new RelationshipTuple.
     *
     * @param targetResource The {@link TargetResource} part of the relationship.
     * @param relation The relation defining the type of connection (e.g., "owner", "editor", "member").
     * @param subject  The {@link Subject} part of the relationship. This can be a direct user or another resource.
     */
    public RelationshipTuple(TargetResource targetResource, String relation, Subject subject) {
        this.targetResource = targetResource;
        this.relation = relation;
        this.subject = subject;
    }

    /**
     * Gets the resource part of the relationship tuple.
     *
     * @return The {@link TargetResource} associated with the tuple.
     */
    public TargetResource getTargetResource() {
        return targetResource;
    }

    /**
     * Gets the relation part of the relationship tuple.
     *
     * @return The relation string.
     */
    public String getRelation() {
        return relation;
    }

    /**
     * Gets the subject part of the relationship tuple.
     *
     * @return The {@link Subject} associated with the tuple.
     */
    public Subject getSubject() {
        return subject;
    }
}
