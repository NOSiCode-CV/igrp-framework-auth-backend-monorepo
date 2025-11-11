package cv.igrp.framework.auth.wso2.role;

import cv.igrp.framework.auth.wso2.application.Wso2ApiErrorHandler;
import cv.igrp.framework.auth.wso2.config.Wso2ServerConfiguration;
import cv.igrp.framework.auth.wso2.group.CreateGroupRequest;
import cv.igrp.framework.auth.wso2.role.dto.AddUserToRoleRequest;
import cv.igrp.framework.auth.wso2.shared.Resource;
import cv.igrp.framework.auth.wso2.shared.ScimResourceSearchResponse;
import cv.igrp.framework.auth.wso2.shared.Wso2ApiResponse;
import cv.igrp.framework.auth.wso2.user.dto.Wso2UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Service
public class RoleService {


    private final WebClient webClient;
    private final Wso2ServerConfiguration serverConfig;

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    public RoleService(WebClient.Builder webClientBuilder, Wso2ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
        this.webClient = webClientBuilder
                .baseUrl(serverConfig.getHost())
                .defaultHeaders(headers ->
                        headers.setBasicAuth(serverConfig.getUsername(), serverConfig.getPassword()))
                .build();
    }

    public Optional<Role> findByName(String name) {
        ResponseEntity<ScimResourceSearchResponse> apiResponse = sendGetRoleByNameRequest(name);
        if (apiResponse != null && apiResponse.getStatusCode().is2xxSuccessful()) {
            ScimResourceSearchResponse responseBody = apiResponse.getBody();
            if (responseBody != null && responseBody.getResources() != null) {
                return responseBody.getResources().stream()
                        .filter(resource -> name.equals(resource.getDisplayName()))
                        .map(resource -> new Role(resource.getId(), resource.getDisplayName()))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    public Wso2ApiResponse<Resource> createRole(Role newRole) {
        log.info("Creating new Role: [{}]", newRole.name());
        ResponseEntity<String> response = sendCreateRoleRequest(new CreateGroupRequest(newRole.name()));
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
            Resource resource = new Resource(applicationId, newRole.name());
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

    public Wso2ApiResponse<Resource> deleteRole(Role foundRole) {
        log.info("Deleting Role: [{}] id: [{}]", foundRole.name(), foundRole.id());
        ResponseEntity<String> deleteRoleResponse = sendDeleteApplicationRequest(foundRole);
        if (deleteRoleResponse != null && !deleteRoleResponse.getStatusCode().is2xxSuccessful()) {
            String errorMessage = Wso2ApiErrorHandler.extractErrorMessage(deleteRoleResponse);
            return Wso2ApiResponse.<Resource>builder()
                    .successfulCall(false)
                    .failureMessage(errorMessage != null ? errorMessage : "No response received from WSO2 IS")
                    .build();
        }
        log.info("Deleted Role id: [{}]", foundRole.id());
        return Wso2ApiResponse.<Resource>builder()
                .successfulCall(true)
                .build();
    }

    public Wso2ApiResponse<Resource> assignRoleToUser(Role role, Wso2UserResponse userResource) {
        log.info("Assigning Role id: [{}] to User id: [{}]", role.id(), userResource.getId());
        AddUserToRoleRequest request = new AddUserToRoleRequest(userResource.getId());
        ResponseEntity<String> response = sendAssignRoleToUser(role, request);
        if (response != null && !response.getStatusCode().is2xxSuccessful()) {
            String errorMessage = Wso2ApiErrorHandler.extractErrorMessage(response);
            return Wso2ApiResponse.<Resource>builder()
                    .successfulCall(false)
                    .failureMessage(errorMessage != null ? errorMessage : "No response received from WSO2 IS")
                    .build();
        }

        return Wso2ApiResponse.<Resource>builder()
                .successfulCall(true)
                .build();
    }

    private ResponseEntity<String> sendDeleteApplicationRequest(Role role) {
        return webClient.delete()
                .uri("/" + serverConfig.getScimBasePath() + "/" + serverConfig.getRolePath() + "/" + role.id())
                .exchangeToMono(response -> response.toEntity(String.class))
                .block();
    }

    private ResponseEntity<String> sendCreateRoleRequest(CreateGroupRequest body) {
        return webClient.post()
                .uri("/" + serverConfig.getScimBasePath() + "/" + serverConfig.getRolePath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response -> response.toEntity(String.class))
                .block();
    }

    private ResponseEntity<ScimResourceSearchResponse> sendGetRoleByNameRequest(String appName) {
        return webClient.get()
                .uri("/" + serverConfig.getScimBasePath() + "/" + serverConfig.getRolePath())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(ScimResourceSearchResponse.class)
                .block();
    }

    private ResponseEntity<String> sendAssignRoleToUser(Role role, AddUserToRoleRequest body) {
        return webClient.patch()
                .uri("/" + serverConfig.getScimBasePath() + "/" + serverConfig.getRolePath() + "/" + role.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response -> response.toEntity(String.class))
                .block();
    }
}
