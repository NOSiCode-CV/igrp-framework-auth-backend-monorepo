package cv.igrp.framework.auth.wso2.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cv.igrp.framework.auth.wso2.config.Wso2ServerConfiguration;
import cv.igrp.framework.auth.wso2.shared.ScimErrorHandler;
import cv.igrp.framework.auth.wso2.shared.Wso2ApiResponse;
import cv.igrp.framework.auth.wso2.user.dto.Wso2UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class UserService {

    private final WebClient webClient;
    private final Wso2ServerConfiguration serverConfig;
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserService(WebClient.Builder webClientBuilder, Wso2ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
        this.webClient = webClientBuilder
                .baseUrl(serverConfig.getHost())
                .defaultHeaders(headers ->
                        headers.setBasicAuth(serverConfig.getUsername(), serverConfig.getPassword()))
                .build();
    }

    public Wso2ApiResponse<Wso2UserResponse> findUserById(String userId) {
        log.info("Finding user with id: [{}]", userId);
        try {
            ResponseEntity<String> response = sendGetUserByIdRequest(userId);
            if (response != null && !response.getStatusCode().is2xxSuccessful()) {
                String errorMessage = ScimErrorHandler.extractErrorMessage(response);
                log.warn("Finding user with id: [{}], error: {}", userId, errorMessage);
                return Wso2ApiResponse.<Wso2UserResponse>builder()
                        .successfulCall(true)
                        .failureMessage(errorMessage)
                        .build();
            }
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                Wso2UserResponse wso2UserResponse = mapper.readValue(response.getBody(), Wso2UserResponse.class);
                return Wso2ApiResponse.<Wso2UserResponse>builder()
                        .resourceResponse(wso2UserResponse)
                        .successfulCall(true)
                        .build();
            }
            log.warn("Finding user with id: [{}], error: {}", userId, "Unexpected Error");
            return Wso2ApiResponse.<Wso2UserResponse>builder()
                    .successfulCall(false)
                    .failureMessage("Unexpected Error.")
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Finding user with id: [{}], error: {}", userId, e.getMessage());
            return Wso2ApiResponse.<Wso2UserResponse>builder()
                    .successfulCall(false)
                    .failureMessage(e.getMessage())
                    .build();
        }
    }

    private ResponseEntity<String> sendGetUserByIdRequest(String userId) {
        return webClient.get()
                .uri("/" + serverConfig.getScimBasePath() + "/" + serverConfig.getUserPath() + "/" + userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> response.toEntity(String.class))
                .block();
    }
}
