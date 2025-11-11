package cv.igrp.framework.auth.permify;


import cv.igrp.framework.auth.core.authorization.model.Decision;
import cv.igrp.framework.auth.core.authorization.model.PermissionCheckRequest;
import cv.igrp.framework.auth.core.authorization.model.RelationshipTuple;
import cv.igrp.framework.auth.core.authorization.model.Subject;
import cv.igrp.framework.auth.core.authorization.model.TargetResource;
import cv.igrp.framework.auth.core.authorization.service.AuthorizationCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

@Component
@SpringBootApplication
public class PermifyHelperRunner implements CommandLineRunner {

    private final AuthorizationCore authorizationCore;
    private static final Logger log = LoggerFactory.getLogger(PermifyHelperRunner.class);

    public PermifyHelperRunner(AuthorizationCore authorizationCore) {
        this.authorizationCore = authorizationCore;
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(PermifyHelperRunner.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Override
    public void run(String... args) {
        revokeResourceViewerFromApplication();
        checkReadPermissionOnResource();
        /*
        grantResourceViewerAtApplicationLevel();
        createTargetResourceRelationships();
        checkUserPermission();
        createTargetResourceRelationshipsAndCheckPermission();
        deleteApplicationAdministratorRelationship();*/
    }

    private void checkUserPermission() {
        System.out.println("\n--- User Permission Check ---");

        String userId = "alpha";
        String appId = "my_alpha_app";

        Subject userSubject = new Subject("user", userId, null);
        TargetResource applicationResource = new TargetResource("application", appId);
        String permissionToTest = "read";

        log.info("Creating relationship: application: {} #creator@user:{}...", appId, userId);
        List<RelationshipTuple> creatorTuple = new ArrayList<>();
        creatorTuple.add(new RelationshipTuple(applicationResource, "creator", userSubject));

        authorizationCore.createRelationships(creatorTuple)
                .thenRun(() -> log.info("Relationship created successfully. application: {} #creator@user: {}", appId, userId))
                .exceptionally(ex -> {
                    log.error("Failed to create relationship: {}", ex.getMessage());
                    throw new CompletionException("Relationship creation failed", ex);
                }).join();

        log.info("Checking if user: {} can: {} application: {}...", userId, permissionToTest, appId);
        PermissionCheckRequest checkRequest = new PermissionCheckRequest(userSubject.getRelation(), applicationResource.getId(), permissionToTest);

        authorizationCore.check(checkRequest).thenAccept(response -> {
            log.info("Result: User '{}' has '{}' on application '{}': {}", userId, permissionToTest, appId, response.getReason());
            log.info("    Reason: {}", response.getReason());
        }).join();
    }


    /**
     * Revokes a subject's resource viewer permission on a specific application by deleting
     * the 'application:applicationId#resource_viewer@user:subjectId' relationship.
     * This method only performs the deletion and does not include permission checks.
     */
    private void revokeResourceViewerFromApplication() {
        String subjectId = "charlie";
        String applicationId = "prod_app";
        log.info("--- Revoking Subject: {} Resource Viewer Access from Application: {}", subjectId, applicationId);
        RelationshipTuple relationshipToDelete = new RelationshipTuple(
                new TargetResource("application", applicationId),
                "resource_viewer",
                new Subject("user", subjectId, null)
        );

        log.info("Attempting to delete relationship: application: {}#resource_viewer@user: {}", applicationId, subjectId);

        authorizationCore.deleteRelationship(relationshipToDelete)
                .thenRun(() -> log.info("Relationship deleted successfully: application: {} #resource_viewer@user:{}", applicationId, subjectId))
                .exceptionally(ex -> {
                    log.error("Failed to delete relationship for: {}. Exception: {}", subjectId, ex.getMessage());
                    throw new CompletionException("Relationship deletion failed", ex);
                }).join();
    }

    private void createTargetResourceRelationships() {
        log.info("--- Creating Resource Relationships ---");

        List<RelationshipTuple> tuplesToCreate = new ArrayList<>();

        String userId = "bos";
        String resourceId = "report_doc";
        String applicationId = "prod_app";

        Subject userSubject = new Subject("user", userId, null);
        TargetResource resourceEntity = new TargetResource("resource", resourceId);

        // 1. Link resource:report_doc to application:prod_app
        tuplesToCreate.add(new RelationshipTuple(
                resourceEntity,
                "application",
                new Subject("application", applicationId, null)
        ));

        // 2. Make user:bos the creator of resource:report_doc
        tuplesToCreate.add(new RelationshipTuple(
                resourceEntity,
                "creator",
                userSubject
        ));

        authorizationCore.createRelationships(tuplesToCreate)
                .thenRun(() -> log.info("Resource relationships created successfully."))
                .exceptionally(ex -> {
                    log.error("Failed to create resource relationships: {}", ex.getMessage());
                    throw new CompletionException("Resource relationship creation failed", ex);
                }).join();
    }

    /**
     * Grants a subject the ability to read 'resource:report_doc' by
     * making that subject a 'resource_viewer' of the associated 'application:prod_app'.
     * This aligns with the 'v1.0' schema's permission 'read = application.resource_viewer or creator'
     * on the 'resource' entity.
     */
    private void grantResourceViewerAtApplicationLevel() {
        log.info("--- Granting Generic Subject Resource Viewer Access at Application Level ---");
        String viewerSubjectId = "charlie";
        List<RelationshipTuple> tuplesToCreate = new ArrayList<>();

        // 'report_doc' belongs to 'prod_app' as per createTargetResourceRelationships()
        String applicationId = "prod_app";

        // Make the generic subject a resource_viewer for 'application:prod_app'
        // Tuple: application:prod_app#resource_viewer@user:viewerSubjectId
        tuplesToCreate.add(new RelationshipTuple(
                new TargetResource("application", applicationId),
                "resource_viewer",
                new Subject("user", viewerSubjectId, null) // Assuming the viewer is a 'user' entity type
        ));
        log.info("Adding tuple: application: {}#resource_viewer@user:{}", applicationId, viewerSubjectId);

        authorizationCore.createRelationships(tuplesToCreate)
                .thenRun(() -> log.info("Resource viewer access granted to: {} on application: {} successfully.", viewerSubjectId, applicationId))
                .exceptionally(ex -> {
                    log.error("Failed to grant:  {} resource viewer access: {}", viewerSubjectId, ex.getMessage());
                    throw new CompletionException("Granting resource viewer access failed", ex);
                }).join();
    }

    private void checkReadPermissionOnResource() {
        log.info("--- Checking Read Permission on 'resource' ---");

        String subjectId = "charlie";
        String resourceId = "report_doc";
        String resourceType = "resource";
        String permission = "read";

        Subject charlieSubject = new Subject("user", subjectId, null);
        TargetResource targetResource = new TargetResource(resourceType, resourceId);

        PermissionCheckRequest checkRequest = new PermissionCheckRequest(
                charlieSubject.getRelation(),
                targetResource.getId(),
                permission
        );

        authorizationCore.check(checkRequest).thenAccept(response -> {
            log.info("Result: Subject '{}' has '{}' on {}:{}: {}", subjectId, permission, resourceType, resourceId, response.getReason());
            log.info("Reason: {}", response.getReason());
        }).join();
    }
}
