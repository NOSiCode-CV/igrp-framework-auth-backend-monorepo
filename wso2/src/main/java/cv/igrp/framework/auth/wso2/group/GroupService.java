package cv.igrp.framework.auth.wso2.group;

import cv.igrp.framework.auth.wso2.config.Wso2ServerConfiguration;
import cv.igrp.framework.auth.wso2.shared.Resource;
import cv.igrp.framework.auth.wso2.shared.ScimResourceSearchResponse;
import cv.igrp.framework.auth.wso2.shared.Wso2ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class GroupService {

    private final Wso2ServerConfiguration serverConfig;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    private static final Logger log = LoggerFactory.getLogger(GroupService.class);

    public GroupService(Wso2ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClients.createDefault();
    }

    public Wso2ApiResponse<Resource> createGroup(String groupName) {
        log.info("Creating application group with name {}", groupName);
        String url = serverConfig.getHost() + "/" + serverConfig.getScimBasePath() + "/" + serverConfig.getGroupPath();
        HttpPost post = new HttpPost(url);
        post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        post.setHeader(HttpHeaders.AUTHORIZATION, buildBasicAuth());

        try {
            CreateGroupRequest request = new CreateGroupRequest(groupName);
            post.setEntity(new StringEntity(objectMapper.writeValueAsString(request), ContentType.APPLICATION_JSON));

            var response = httpClient.execute(post);
            int statusCode = response.getCode();

            if (statusCode >= 200 && statusCode < 300) {
                String location = response.getHeader(HttpHeaders.LOCATION) != null
                        ? response.getHeader(HttpHeaders.LOCATION).getValue()
                        : null;

                if (location != null) {
                    String id = location.substring(location.lastIndexOf('/') + 1);
                    return Wso2ApiResponse.<Resource>builder()
                            .successfulCall(true)
                            .resourceResponse(new Resource(id, groupName))
                            .build();
                } else {
                    log.warn("Group created but no Location header returned.");
                    return Wso2ApiResponse.<Resource>builder()
                            .successfulCall(true)
                            .failureMessage("Group created, but no Location header returned")
                            .build();
                }
            } else {
                return buildErrorResponse("Failed to create group. Status: " + statusCode);
            }

        } catch (Exception e) {
            return buildErrorResponse("Error creating group: " + e.getMessage());
        }
    }

    public Wso2ApiResponse<Resource> deleteGroup(Resource resource) {
        log.info("Deleting group with ID: {}", resource.id());
        String url = serverConfig.getHost() + "/" + serverConfig.getScimBasePath() + "/" + serverConfig.getGroupPath() + "/" + resource.id();
        HttpDelete delete = new HttpDelete(url);
        delete.setHeader(HttpHeaders.AUTHORIZATION, buildBasicAuth());

        try {
            var response = httpClient.execute(delete);
            int statusCode = response.getCode();

            if (statusCode >= 200 && statusCode < 300) {
                return Wso2ApiResponse.<Resource>builder()
                        .successfulCall(true)
                        .build();
            } else {
                return buildErrorResponse("Failed to delete group. Status: " + statusCode);
            }

        } catch (Exception e) {
            return buildErrorResponse("Error deleting group: " + e.getMessage());
        }
    }

    public Optional<Resource> getGroupByName(String groupName) {
        log.info("Searching for group with name {}", groupName);
        String filter = serverConfig.getGroupFilter() + groupName;
        String url = serverConfig.getHost() + "/" + serverConfig.getScimBasePath() + "/" + serverConfig.getGroupPath() + filter;

        HttpGet get = new HttpGet(url);
        get.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
        get.setHeader(HttpHeaders.AUTHORIZATION, buildBasicAuth());

        try (var response = httpClient.execute(get)) {
            int statusCode = response.getCode();

            if (statusCode >= 200 && statusCode < 300) {
                var responseBody = response.getEntity() != null ? response.getEntity().getContent() : null;
                ScimResourceSearchResponse body = objectMapper.readValue(responseBody, ScimResourceSearchResponse.class);

                return body.getResources().stream()
                        .filter(resource -> groupName.equals(resource.getDisplayName()))
                        .map(resource -> new Resource(resource.getId(), resource.getDisplayName()))
                        .findFirst();
            }

            log.error("Failed to get group. Status: {}", statusCode);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error searching for group: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Wso2ApiResponse<Resource> updateGroupName(Resource resource, Resource newResourceData) {
        log.info("Updating group [{}] to new name [{}]", resource.id(), newResourceData.name());

        String url = serverConfig.getHost() + "/" + serverConfig.getScimBasePath() + "/" + serverConfig.getGroupPath() + "/" + resource.id();
        HttpPatch patch = new HttpPatch(url);
        patch.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        patch.setHeader(HttpHeaders.AUTHORIZATION, buildBasicAuth());

        try {
            UpdateGroupRequest request = new UpdateGroupRequest(newResourceData.name());
            patch.setEntity(new StringEntity(objectMapper.writeValueAsString(request), ContentType.APPLICATION_JSON));

            var response = httpClient.execute(patch);
            int statusCode = response.getCode();

            if (statusCode >= 200 && statusCode < 300) {
                return Wso2ApiResponse.<Resource>builder()
                        .successfulCall(true)
                        .build();
            } else {
                return buildErrorResponse("Failed to update group. Status: " + statusCode);
            }

        } catch (IOException e) {
            return buildErrorResponse("Error updating group: " + e.getMessage());
        }
    }

    private Wso2ApiResponse<Resource> buildErrorResponse(String msg) {
        return Wso2ApiResponse.<Resource>builder()
                .successfulCall(false)
                .failureMessage(msg)
                .build();
    }

    private String buildBasicAuth() {
        String auth = serverConfig.getUsername() + ":" + serverConfig.getPassword();
        return "Basic " + java.util.Base64.getEncoder().encodeToString(auth.getBytes());
    }
}
