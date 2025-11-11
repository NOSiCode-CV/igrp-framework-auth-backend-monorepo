package cv.igrp.framework.auth.core.authorization.model;

/**
 * Represents a **Subject** in the authorization system.
 * A Subject is an {@link Entity} that attempts to perform an action.
 * This could be a user, a group, a service account, or even another resource
 * if it acts as a subject in a relationship (e.g., "group:finance#member").
 * It extends {@link Entity} and can optionally include a specific relation
 * if it represents a subject set (e.g., "group:finance#member").
 *
 * @see Entity
 */
public class Subject extends Entity {
    private String relation;

    /**
     * Constructs a new Subject with a type, identifier, and an optional relation.
     * This constructor is typically used when the subject itself is part of a subject set,
     * such as a "member" of a "group".
     *
     * @param type     The categorization or kind of the subject (e.g., "user", "group", "service_account").
     * @param id       The unique identifier of the specific subject instance (e.g., "johndoe", "finance_group_id").
     * @param relation An optional relation indicating membership or a specific role within the subject entity itself
     * (e.g., "member" if the subject is "group:finance#member"). Can be null if not applicable.
     */
    public Subject(String type, String id, String relation) {
        super(type, id);
        this.relation = relation;
    }

    /**
     * Constructs a new Subject with only a relation.
     * This constructor is typically used for defining a subject set where the
     * actual type and ID are implicitly derived or not directly needed for this specific
     * representation (e.g., when a subject refers to a relationship on another subject).
     *
     * @param relation The relation indicating membership or a specific role within the subject entity itself
     * (e.g., "member" if the subject is "group:finance#member").
     */
    public Subject(String relation) {
        this.relation = relation;
    }

    /**
     * Retrieves the optional relation associated with the subject.
     * This is used when the subject itself is a subject set (e.g., "group:finance#member").
     *
     * @return The relation string, or {@code null} if not applicable.
     */
    public String getRelation() {
        return relation;
    }

    /**
     * Sets the optional relation for the subject.
     *
     * @param relation The new relation string.
     */
    public void setRelation(String relation) {
        this.relation = relation;
    }
}
