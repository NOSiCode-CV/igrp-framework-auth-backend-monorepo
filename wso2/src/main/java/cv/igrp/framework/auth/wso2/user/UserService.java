package cv.igrp.framework.auth.wso2.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cv.igrp.framework.auth.wso2.config.Wso2ServerConfiguration;
import cv.igrp.framework.auth.wso2.shared.ScimErrorHandler;
import cv.igrp.framework.auth.wso2.shared.Wso2ApiResponse;
import cv.igrp.framework.auth.wso2.user.dto.Wso2UserResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class UserService {

    private final CloseableHttpClient httpClient;
    private final Wso2ServerConfiguration serverConfig;
    private final ObjectMapper mapper;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserService(Wso2ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
        this.httpClient = HttpClients.createDefault();
        this.mapper = new ObjectMapper();
    }

    public Wso2ApiResponse<Wso2UserResponse> findUserById(String userId) {
        log.info("Finding user with id: [{}]", userId);
        try {
            HttpGet get = new HttpGet(serverConfig.getHost() + "/" + serverConfig.getScimBasePath() + "/" + serverConfig.getUserPath() + "/" + userId);
            get.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
            get.setHeader(HttpHeaders.AUTHORIZATION, buildBasicAuth());

            try (var response = httpClient.execute(get)) {
                int code = response.getCode();
                InputStream content = response.getEntity().getContent();
                String body = new String(content.readAllBytes());

                if (code >= 200 && code < 300) {
                    Wso2UserResponse user = mapper.readValue(body, Wso2UserResponse.class);
                    return Wso2ApiResponse.<Wso2UserResponse>builder()
                            .successfulCall(true)
                            .resourceResponse(user)
                            .build();
                } else {
                    String errorMessage = ScimErrorHandler.extractErrorMessage(body);
                    log.warn("Finding user with id: [{}], error: {}", userId, errorMessage);
                    return Wso2ApiResponse.<Wso2UserResponse>builder()
                            .successfulCall(false)
                            .failureMessage(errorMessage)
                            .build();
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Finding user with id: [{}], error: {}", userId, e.getMessage());
            return Wso2ApiResponse.<Wso2UserResponse>builder()
                    .successfulCall(false)
                    .failureMessage(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Finding user with id: [{}], unexpected error: {}", userId, e.getMessage());
            return Wso2ApiResponse.<Wso2UserResponse>builder()
                    .successfulCall(false)
                    .failureMessage("Unexpected Error: " + e.getMessage())
                    .build();
        }
    }

    private String buildBasicAuth() {
        String raw = serverConfig.getUsername() + ":" + serverConfig.getPassword();
        return "Basic " + java.util.Base64.getEncoder().encodeToString(raw.getBytes());
    }
}
