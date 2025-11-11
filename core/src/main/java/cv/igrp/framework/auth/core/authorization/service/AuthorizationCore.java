package cv.igrp.framework.auth.core.authorization.service;


import cv.igrp.framework.auth.core.authorization.model.PermissionCheckRequest;
import cv.igrp.framework.auth.core.authorization.model.PermissionCheckResponse;
import cv.igrp.framework.auth.core.authorization.model.RelationshipTuple;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Interface for interacting with Zanzibar-like authorization systems (e.g., Permify, Permit.io).
 * This abstracts the core operations for managing and checking permissions based on relationships.
 * Designed to be used as a library within the main application.
 */
public interface AuthorizationCore {

    /**
     * Checks if a subject has a specific permission (relation) on a resource.
     * This represents the check: "Can a subject perform a relation on a resource?".
     *
     * @param checkRequest The request containing subject, relation, and resource details.
     * @return A CompletableFuture that completes with a {@link PermissionCheckResponse}
     * indicating the authorization decision and an optional reason.
     */
    CompletableFuture<PermissionCheckResponse> check(PermissionCheckRequest checkRequest);

    /**
     * Creates one or more relationships (tuples) in the authorization system.
     * Supports batch creation for efficiency.
     *
     * @param relationshipTuples A {@link List} of {@link RelationshipTuple}
     * objects to be created.
     * @return A {@link CompletableFuture} that completes when all relationships are successfully created.
     * @throws CompletionException if the relationship write operation fails.
     */
    CompletableFuture<Void> createRelationships(List<RelationshipTuple> relationshipTuples);

    /**
     * Deletes a single, specific relationship (tuple) from the authorization system.
     * The provided relationship tuple will be precisely matched and removed.
     *
     * @param relationshipTupleToDelete The {@link RelationshipTuple} object representing the exact relationship to be deleted.
     * @return A {@link CompletableFuture} that completes with {@code null} upon successful deletion.
     * It completes exceptionally if the deletion operation fails.
     * @throws CompletionException if the relationship deletion operation fails.
     */
    CompletableFuture<Void> deleteRelationship(RelationshipTuple relationshipTupleToDelete);

}
