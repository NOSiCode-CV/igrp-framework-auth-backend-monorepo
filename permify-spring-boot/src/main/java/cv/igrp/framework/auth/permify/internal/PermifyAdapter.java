package cv.igrp.framework.auth.permify.internal;

import cv.igrp.framework.auth.core.authorization.model.Decision;
import cv.igrp.framework.auth.core.authorization.model.PermissionCheckRequest;
import cv.igrp.framework.auth.core.authorization.model.PermissionCheckResponse;
import cv.igrp.framework.auth.core.authorization.model.RelationshipTuple;
import cv.igrp.framework.auth.core.authorization.service.AuthorizationCore;
import org.permify.api.DataApi;
import org.permify.api.PermissionApi;
import org.permify.model.AttributeFilter;
import org.permify.model.CheckResult;
import org.permify.model.Context;
import org.permify.model.DataDeleteRequest;
import org.permify.model.DataWriteRequest;
import org.permify.model.DataWriteRequestMetadata;
import org.permify.model.Entity;
import org.permify.model.EntityFilter;
import org.permify.model.PermissionCheckRequestMetadata;
import org.permify.model.PermissionsCheckRequest;
import org.permify.model.Subject;
import org.permify.model.SubjectFilter;
import org.permify.model.Tuple;
import org.permify.model.TupleFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;


public class PermifyAdapter implements AuthorizationCore {

    public static final int DEFAULT_DEPTH = 20;
    private static final Logger LOG = LoggerFactory.getLogger(PermifyAdapter.class);
    private final PermissionApi permissionApi;
    private final DataApi dataApi;
    private final String TENANT_ID = "default";

    public PermifyAdapter(PermissionApi permissionApi, DataApi dataApi) {
        this.permissionApi = permissionApi;
        this.dataApi = dataApi;
    }

    @Override
    public CompletableFuture<PermissionCheckResponse> check(PermissionCheckRequest checkRequest){
        LOG.info("Request to Check Permission - Subject: {} Relation: {} Resource: {}",
                checkRequest.getSubject(), checkRequest.getResource(), checkRequest.getResource());

        if (checkRequest.getSubject() == null ||
                checkRequest.getResource() == null || checkRequest.getResource() == null) {
            LOG.warn("Invalid permission check request. Subject, Relation, or Resource is null. Returning DENIED.");
            PermissionCheckResponse denied = new PermissionCheckResponse();
            //denied.setDecision(Decision.DENIED);
            denied.setReason("Invalid request - missing required fields");
            return CompletableFuture.completedFuture(denied);
        }

        Entity entity = new Entity()
                .id(checkRequest.getResource())
                .type(checkRequest.getResource());

        Subject subject = new Subject()
                .id(checkRequest.getSubject())
                .type(checkRequest.getSubject());

        // Handle optional subject relation
        if (checkRequest.getSubject() != null && !checkRequest.getSubject().isEmpty()) {
            subject.setRelation(checkRequest.getSubject());
        }

        PermissionsCheckRequest requestBody = new PermissionsCheckRequest()
                .entity(entity)
                .permission(checkRequest.getAction())
                .subject(subject)
                .metadata(new PermissionCheckRequestMetadata().depth(DEFAULT_DEPTH));

        /*if (checkRequest.getContextTuples() != null && !checkRequest.getContextTuples().isEmpty()) {
            Context context = new Context();
            context.setTuples(checkRequest.getContextTuples().stream()
                    .map(this::mapToPermifyTuple)
                    .collect(Collectors.toList()));
            requestBody.setContext(context);
        }*/

        return permissionApi.permissionsCheck(TENANT_ID, requestBody)
                .map(response -> {
                    PermissionCheckResponse result = new PermissionCheckResponse();
                    if (response.getCan() == CheckResult.ALLOWED) {
                        result.setAllowed(true);
                        result.setReason("Permission granted by authorization server.");
                    } else {
                        result.setAllowed(false);
                        result.setReason("Permission denied by authorization server.");
                    }
                    return result;
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    LOG.error("Permify permission check error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    PermissionCheckResponse errorResponse = new PermissionCheckResponse();
                    errorResponse.setAllowed(false);
                    errorResponse.setReason("Permify permission check failed: " + e.getMessage());
                    return Mono.just(errorResponse);
                })
                .onErrorResume(Exception.class, e -> {
                    LOG.warn("Unexpected error during Permify permission check: {}", e.getMessage());
                    PermissionCheckResponse errorResponse = new PermissionCheckResponse();
                    errorResponse.setAllowed(false);
                    errorResponse.setReason("Unexpected error during permission check: " + e.getMessage());
                    return Mono.just(errorResponse);
                })
                .toFuture();
    }

    @Override
    public CompletableFuture<Void> createRelationships(List<RelationshipTuple> relationshipTuples) {
        LOG.info("Request to create {} relationships.", relationshipTuples.size());

        if (relationshipTuples.isEmpty()) {
            LOG.warn("No relationships provided for creation. Skipping.");
            return CompletableFuture.completedFuture(null);
        }

        List<Tuple> permifyTuples = relationshipTuples.stream()
                .map(this::mapToPermifyTuple)
                .collect(Collectors.toList());

        DataWriteRequest requestBody = new DataWriteRequest();
        requestBody.setTuples(permifyTuples);
        requestBody.setMetadata(new DataWriteRequestMetadata());

        return dataApi.dataWrite(TENANT_ID, requestBody)
                .doOnSuccess(response -> LOG.info("Successfully created {} relationships in Permify.", relationshipTuples.size()))
                .onErrorResume(WebClientResponseException.class, e -> {
                    LOG.error("Permify relationships write error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.error(new CompletionException("Permify relationships write failed", e));
                })
                .onErrorResume(Exception.class, e -> {
                    LOG.warn("Unexpected error during Permify relationships write: {}", e.getMessage());
                    return Mono.error(new CompletionException("Unexpected error with Permify relationships write", e));
                })
                .then()
                .toFuture();
    }

    @Override
    public CompletableFuture<Void> deleteRelationship(RelationshipTuple relationshipTupleToDelete) {
        if (relationshipTupleToDelete == null) {
            LOG.warn("No relationship provided for deletion. Skipping.");
            return CompletableFuture.completedFuture(null);
        }

        TupleFilter filter = mapToPermifyTupleFilter(relationshipTupleToDelete);
        DataDeleteRequest requestBody = new DataDeleteRequest();
        requestBody.setTupleFilter(filter);
        requestBody.setAttributeFilter(new AttributeFilter());

        return dataApi.dataDelete(TENANT_ID, requestBody)
                .doOnSuccess(response -> LOG.info("Successfully deleted relationship: {}", formatTupleForLog(relationshipTupleToDelete)))
                .onErrorResume(WebClientResponseException.class, e -> {
                    LOG.error("Permify relationship delete error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.error(new CompletionException("Permify relationship delete failed", e));
                })
                .onErrorResume(Exception.class, e -> {
                    LOG.warn("Unexpected error during Permify relationship delete: {}", e.getMessage());
                    return Mono.error(new CompletionException("Unexpected error with Permify relationship delete", e));
                })
                .then()
                .toFuture();
    }

    /**
     * Helper method to map your custom RelationshipTuple DTO to Permify's Tuple model.
     *
     * @param relationshipTuple Your custom RelationshipTuple object.
     * @return Permify's Tuple object.
     */
    private Tuple mapToPermifyTuple(RelationshipTuple relationshipTuple) {
        Tuple permifyTuple = new Tuple();

        Entity entity = new Entity()
                .id(relationshipTuple.getTargetResource().getId())
                .type(relationshipTuple.getTargetResource().getType());
        permifyTuple.setEntity(entity);

        permifyTuple.setRelation(relationshipTuple.getRelation());

        Subject permifySubject = new Subject()
                .id(relationshipTuple.getSubject().getId())
                .type(relationshipTuple.getSubject().getType());

        if (relationshipTuple.getSubject().getRelation() != null && !relationshipTuple.getSubject().getRelation().isEmpty()) {
            permifySubject.setRelation(relationshipTuple.getSubject().getRelation());
        }
        permifyTuple.setSubject(permifySubject);

        return permifyTuple;
    }

    /**
     * Helper method to map your custom RelationshipTuple DTO to Permify's TupleFilter model (for deleting).
     * This creates a precise filter for an exact tuple.
     */
    private TupleFilter mapToPermifyTupleFilter(RelationshipTuple relationshipTuple) {
        TupleFilter filter = new TupleFilter();

        EntityFilter entityFilter = new EntityFilter();
        entityFilter.setType(relationshipTuple.getTargetResource().getType());
        entityFilter.addIdsItem(relationshipTuple.getTargetResource().getId());
        filter.setEntity(entityFilter);

        filter.setRelation(relationshipTuple.getRelation());

        // Create and set SubjectFilter if required.
        if (relationshipTuple.getSubject().getRelation() != null && !relationshipTuple.getSubject().getRelation().isEmpty()) {
            SubjectFilter subjectFilter = new SubjectFilter();
            subjectFilter.setType(relationshipTuple.getSubject().getType());
            subjectFilter.addIdsItem(relationshipTuple.getSubject().getId());
            subjectFilter.setRelation(relationshipTuple.getSubject().getRelation());
            filter.setSubject(subjectFilter);
        }

        return filter;
    }

    /**
     * Helper method to format a RelationshipTuple for logging purposes.
     */
    private String formatTupleForLog(RelationshipTuple tuple) {
        return String.format("%s:%s#%s@%s:%s%s",
                tuple.getTargetResource(), tuple.getTargetResource(),
                tuple.getRelation(),
                tuple.getSubject(), tuple.getSubject(),
                (tuple.getSubject().getRelation() != null && !tuple.getSubject().getRelation().isEmpty() ? "#" + tuple.getSubject().getRelation() : "")
        );
    }
}
