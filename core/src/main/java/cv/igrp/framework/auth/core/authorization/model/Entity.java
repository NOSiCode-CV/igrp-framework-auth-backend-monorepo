package cv.igrp.framework.auth.core.authorization.model;

/**
 * Abstract base class for all entities involved in authorization,
 * such as {@link TargetResource}s and {@link Subject}s.
 * It provides common properties like a type and an identifier.
 */
public abstract class Entity {
    private String type;
    private String id;

    /**
     * Default constructor for Entity.
     */
    public Entity() {
    }

    /**
     * Constructs a new Entity with a specified type and identifier.
     *
     * @param type The categorization or kind of the entity (e.g., "user", "document", "organization").
     * @param id   The unique identifier of the specific entity instance (e.g., "johndoe", "doc_123", "org_abc").
     */
    public Entity(String type, String id) {
        this.type = type;
        this.id = id;
    }

    /**
     * Retrieves the type of the entity.
     *
     * @return The type of the entity.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the entity.
     *
     * @param type The new type for the entity.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Retrieves the unique identifier of the entity.
     *
     * @return The ID of the entity.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the entity.
     *
     * @param id The new ID for the entity.
     */
    public void setId(String id) {
        this.id = id;
    }
}
