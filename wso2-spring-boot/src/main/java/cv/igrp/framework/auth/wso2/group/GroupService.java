package cv.igrp.framework.auth.wso2.group;

import cv.igrp.framework.auth.wso2.application.Wso2ApiErrorHandler;
import cv.igrp.framework.auth.wso2.config.Wso2ServerConfiguration;
import cv.igrp.framework.auth.wso2.shared.Resource;
import cv.igrp.framework.auth.wso2.shared.ScimResourceSearchResponse;
import cv.igrp.framework.auth.wso2.shared.Wso2ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Service
public class GroupService {

    private final WebClient webClient;
    private final Wso2ServerConfiguration serverConfig;

    private static final Logger log = LoggerFactory.getLogger(GroupService.class);

    public GroupService(WebClient.Builder webClientBuilder, Wso2ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
        this.webClient = webClientBuilder
                .baseUrl(serverConfig.getHost())
                .defaultHeaders(headers ->
                        headers.setBasicAuth(serverConfig.getUsername(), serverConfig.getPassword()))
                .build();
    }

    public Wso2ApiResponse<Resource> createGroup(String groupName) {
        log.info("Creating application group with name {}", groupName);
        ResponseEntity<String> response = sendCreateApplicationGroupRequest(new CreateGroupRequest(groupName));
        if (response != null && !response.getStatusCode().is2xxSuccessful()) {
            String errorMessage = Wso2ApiErrorHandler.extractErrorMessage(response);
            return Wso2ApiResponse.<Resource>builder()
                    .successfulCall(false)
                    .failureMessage(errorMessage != null ? errorMessage : "No response received from WSO2 IS")
                    .build();
        }
        String location = Optional.ofNullable(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .orElse(null);

        if (location != null) {
            String applicationId = location.substring(location.lastIndexOf('/') + 1);
            Resource resource = new Resource(applicationId, groupName);
            return Wso2ApiResponse.<Resource>builder()
                    .successfulCall(true)
                    .resourceResponse(resource)
                    .build();
        }
        log.warn("Application created, but no Location header was returned.");
        return Wso2ApiResponse.<Resource>builder()
                .successfulCall(true)
                .failureMessage("Application created, but no Location header returned")
                .build();
    }

    public Wso2ApiResponse<Resource> deleteGroup(Resource resource) {
        log.info("Deleting application group with id: [{}]", resource.id());
        ResponseEntity<String> response = sendDeleteApplicationRequest(resource);
        if (response != null && !response.getStatusCode().is2xxSuccessful()) {
            String errorMessage = Wso2ApiErrorHandler.extractErrorMessage(response);
            return Wso2ApiResponse.<Resource>builder()
                    .successfulCall(false)
                    .failureMessage(errorMessage != null ? errorMessage : "No response received from WSO2 IS")
                    .build();
        }
        log.info("Deleted application group with id: [{}]", resource.id());
        return Wso2ApiResponse.<Resource>builder()
                .successfulCall(true)
                .build();
    }

    public Optional<Resource> getGroupByName(String groupName) {
        log.info("Searching for group with name {}", groupName);
        ResponseEntity<ScimResourceSearchResponse> response = sendSearchGroupByNameRequest(groupName);
        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            log.error("Failed to fetch application [{}]. Status: {}", groupName,
                    response != null ? response.getStatusCode() : "null response");
            return Optional.empty();
        }

        ScimResourceSearchResponse body = response.getBody();
        if (body == null || body.getResources() == null) {
            log.error("Empty body returned for application [{}]", groupName);
            return Optional.empty();
        }

        return body.getResources().stream()
                .filter(resource -> resource.getDisplayName().equals(groupName))
                .map(resource -> new Resource(resource.getId(), resource.getDisplayName()))
                .findFirst()
                .or(() -> {
                    log.warn("Group: [{}] not found in search results", groupName);
                    return Optional.empty();
                });
    }

    public Wso2ApiResponse<Resource> updateGroupName(Resource resource, Resource newResourceData) {
        log.info("Updating Application Group name: [{}], id [{}]. New Name [{}]", resource.name(), resource.id(), newResourceData.name());
        UpdateGroupRequest request = new UpdateGroupRequest(newResourceData.name());
        ResponseEntity<String> response = sendUpdateApplicationGroupRequest(resource, request);
        if (response != null && !response.getStatusCode().is2xxSuccessful()) {
            String errorMessage = Wso2ApiErrorHandler.extractErrorMessage(response);
            return Wso2ApiResponse.<Resource>builder()
                    .successfulCall(false)
                    .failureMessage(errorMessage != null ? errorMessage : "No response received from WSO2 IS")
                    .build();
        }
        log.info("Updated Application Group with id: [{}].", newResourceData.id());
        return Wso2ApiResponse.<Resource>builder()
                .successfulCall(true)
                .build();
    }

    private ResponseEntity<String> sendCreateApplicationGroupRequest(CreateGroupRequest body) {
        return webClient.post()
                .uri("/" + serverConfig.getScimBasePath() + "/" + serverConfig.getGroupPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response -> response.toEntity(String.class))
                .block();
    }

    private ResponseEntity<ScimResourceSearchResponse> sendSearchGroupByNameRequest(String groupName) {
        String filterQuery = serverConfig.getGroupFilter() + groupName;
        return webClient.get()
                .uri("/" + serverConfig.getScimBasePath() + "/" + serverConfig.getGroupPath() + filterQuery)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(ScimResourceSearchResponse.class)
                .block();
    }

    private ResponseEntity<String> sendDeleteApplicationRequest(Resource resource) {
        return webClient.delete()
                .uri("/" + serverConfig.getScimBasePath() + "/" + serverConfig.getGroupPath() + "/" + resource.id())
                .exchangeToMono(response -> response.toEntity(String.class))
                .block();
    }

    private ResponseEntity<String> sendUpdateApplicationGroupRequest(Resource resource, UpdateGroupRequest body) {
        return webClient.patch()
                .uri("/" + serverConfig.getScimBasePath() + "/" + serverConfig.getGroupPath() + "/" + resource.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response -> response.toEntity(String.class))
                .block();
    }
}
