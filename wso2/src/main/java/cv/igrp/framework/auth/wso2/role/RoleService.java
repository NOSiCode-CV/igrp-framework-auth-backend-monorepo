package cv.igrp.framework.auth.wso2.role;

import com.fasterxml.jackson.databind.ObjectMapper;
import cv.igrp.framework.auth.wso2.config.Wso2ServerConfiguration;
import cv.igrp.framework.auth.wso2.group.CreateGroupRequest;
import cv.igrp.framework.auth.wso2.role.dto.AddUserToRoleRequest;
import cv.igrp.framework.auth.wso2.shared.Resource;
import cv.igrp.framework.auth.wso2.shared.ScimResourceSearchResponse;
import cv.igrp.framework.auth.wso2.shared.Wso2ApiResponse;
import cv.igrp.framework.auth.wso2.user.dto.Wso2UserResponse;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Optional;

public class RoleService {

    private final Wso2ServerConfiguration serverConfig;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper mapper;

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    public RoleService(Wso2ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
        this.httpClient = HttpClients.createDefault();
        this.mapper = new ObjectMapper();
    }

    public Optional<Role> findByName(String name) {
        try {
            String url = serverConfig.getHost() + "/" + serverConfig.getScimBasePath() + "/" + serverConfig.getRolePath();
            HttpGet get = new HttpGet(url);
            get.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
            get.setHeader(HttpHeaders.AUTHORIZATION, buildBasicAuth());

            try (var response = httpClient.execute(get)) {
                if (response.getCode() >= 200 && response.getCode() < 300) {
                    InputStream body = response.getEntity().getContent();
                    ScimResourceSearchResponse result = mapper.readValue(body, ScimResourceSearchResponse.class);
                    return result.getResources().stream()
                            .filter(r -> name.equals(r.getDisplayName()))
                            .map(r -> new Role(r.getId(), r.getDisplayName()))
                            .findFirst();
                }
            }
        } catch (Exception e) {
            log.error("Error finding role: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Wso2ApiResponse<Resource> createRole(Role role) {
        log.info("Creating new role: {}", role.name());
        try {
            String url = serverConfig.getHost() + "/" + serverConfig.getScimBasePath() + "/" + serverConfig.getRolePath();
            HttpPost post = new HttpPost(url);
            post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
            post.setHeader(HttpHeaders.AUTHORIZATION, buildBasicAuth());
            post.setEntity(new StringEntity(mapper.writeValueAsString(new CreateGroupRequest(role.name())), ContentType.APPLICATION_JSON));

            try (var response = httpClient.execute(post)) {
                if (response.getCode() >= 200 && response.getCode() < 300) {
                    String location = response.getHeader(HttpHeaders.LOCATION) != null ? response.getHeader(HttpHeaders.LOCATION).getValue() : null;
                    if (location != null) {
                        String id = location.substring(location.lastIndexOf('/') + 1);
                        return Wso2ApiResponse.<Resource>builder().successfulCall(true).resourceResponse(new Resource(id, role.name())).build();
                    } else {
                        return Wso2ApiResponse.<Resource>builder().successfulCall(true).failureMessage("No Location header").build();
                    }
                } else {
                    return Wso2ApiResponse.<Resource>builder().successfulCall(false).failureMessage("Failed with status: " + response.getCode()).build();
                }
            }
        } catch (Exception e) {
            return Wso2ApiResponse.<Resource>builder().successfulCall(false).failureMessage(e.getMessage()).build();
        }
    }

    public Wso2ApiResponse<Resource> deleteRole(Role role) {
        log.info("Deleting role id: {}", role.id());
        try {
            String url = serverConfig.getHost() + "/" + serverConfig.getScimBasePath() + "/" + serverConfig.getRolePath() + "/" + role.id();
            HttpDelete delete = new HttpDelete(url);
            delete.setHeader(HttpHeaders.AUTHORIZATION, buildBasicAuth());

            try (var response = httpClient.execute(delete)) {
                if (response.getCode() >= 200 && response.getCode() < 300) {
                    return Wso2ApiResponse.<Resource>builder().successfulCall(true).build();
                }
                return Wso2ApiResponse.<Resource>builder().successfulCall(false).failureMessage("Failed to delete").build();
            }
        } catch (Exception e) {
            return Wso2ApiResponse.<Resource>builder().successfulCall(false).failureMessage(e.getMessage()).build();
        }
    }

    public Wso2ApiResponse<Resource> assignRoleToUser(Role role, Wso2UserResponse user) {
        log.info("Assigning role {} to user {}", role.id(), user.getId());
        try {
            String url = serverConfig.getHost() + "/" + serverConfig.getScimBasePath() + "/" + serverConfig.getRolePath() + "/" + role.id();
            HttpPatch patch = new HttpPatch(url);
            patch.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
            patch.setHeader(HttpHeaders.AUTHORIZATION, buildBasicAuth());

            AddUserToRoleRequest body = new AddUserToRoleRequest(user.getId());
            patch.setEntity(new StringEntity(mapper.writeValueAsString(body), ContentType.APPLICATION_JSON));

            try (var response = httpClient.execute(patch)) {
                if (response.getCode() >= 200 && response.getCode() < 300) {
                    return Wso2ApiResponse.<Resource>builder().successfulCall(true).build();
                } else {
                    return Wso2ApiResponse.<Resource>builder().successfulCall(false).failureMessage("Failed to assign role").build();
                }
            }
        } catch (Exception e) {
            return Wso2ApiResponse.<Resource>builder().successfulCall(false).failureMessage(e.getMessage()).build();
        }
    }

    private String buildBasicAuth() {
        String raw = serverConfig.getUsername() + ":" + serverConfig.getPassword();
        return "Basic " + java.util.Base64.getEncoder().encodeToString(raw.getBytes());
    }
}