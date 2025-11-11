package cv.igrp.framework.auth.wso2.application;

import cv.igrp.framework.auth.wso2.application.dto.ApplicationSearchResponse;
import cv.igrp.framework.auth.wso2.application.dto.CreateApplicationRequest;
import cv.igrp.framework.auth.wso2.application.dto.UpdateApplicationRequest;
import cv.igrp.framework.auth.wso2.config.Wso2ServerConfiguration;
import cv.igrp.framework.auth.wso2.shared.Wso2ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

public class ApplicationService {

    private final Wso2ServerConfiguration serverConfig;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    public ApplicationService(Wso2ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }

    public Wso2ApiResponse<Application> createApplication(CreateApplicationRequest request) {
        log.info("Creating application with name {}", request.getName());
        HttpPost httpPost = new HttpPost(buildUrl(serverConfig.getApplicationPath()));
        try {
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(request), ContentType.APPLICATION_JSON));

            try (var response = httpClient.execute(httpPost)) {
                int status = response.getCode();
                var headers = response.getHeaders();
                InputStream content = response.getEntity().getContent();
                String body = new String(content.readAllBytes());
                if (status >= 200 && status < 300) {
                    String location = Arrays.stream(headers).filter(it -> Objects.equals(it.getName(), HttpHeaders.LOCATION)).findFirst()
                            .map(NameValuePair::getValue).orElse(null);

                    if (location != null) {
                        String appId = location.substring(location.lastIndexOf('/') + 1);
                        return Wso2ApiResponse.<Application>builder()
                                .successfulCall(true)
                                .resourceResponse(new Application(appId, request.getName()))
                                .build();
                    }
                    return Wso2ApiResponse.<Application>builder()
                            .successfulCall(true)
                            .failureMessage("Application created, but no Location header returned")
                            .build();
                } else {
                    String errorMessage = Wso2ApiErrorHandler.extractErrorMessage(body);
                    log.warn("Create application with name: [{}], error: {}", request.getName(), errorMessage);
                    return Wso2ApiResponse.<Application>builder()
                            .successfulCall(false)
                            .failureMessage("Failed with status " + status)
                            .build();
                }
            }
        } catch (IOException e) {
            return handleException(e);
        }
    }

    public Optional<Application> getApplicationByName(Application application) {
        log.info("Searching for application with name: {}", application.name());
        String filter = serverConfig.getApplicationFilter() + application.name();
        HttpGet httpGet = new HttpGet(buildUrl(serverConfig.getApplicationPath() + filter));
        try {
            httpGet.setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
            httpGet.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());

            try (var response = httpClient.execute(httpGet)) {

                if (response.getCode() != 200) {
                    InputStream content = response.getEntity().getContent();
                    String anyBody = new String(content.readAllBytes());
                    String errorMessage = Wso2ApiErrorHandler.extractErrorMessage(anyBody);
                    log.warn("Failed to fetch Application with name: [{}], error: {}", application.name(), errorMessage);
                    return Optional.empty();
                }

                var body = objectMapper.readValue(response.getEntity().getContent(), ApplicationSearchResponse.class);
                return body.getApplications().stream()
                        .filter(app -> application.name().equals(app.getName()))
                        .map(app -> new Application(app.getId(), app.getName()))
                        .findFirst();
            }
        } catch (IOException e) {
            log.error("Error during getApplicationByName", e);
            return Optional.empty();
        }
    }

    public Wso2ApiResponse<Application> deleteApplication(Application application) {
        log.info("Deleting application with id {}", application.id());
        HttpDelete httpDelete = new HttpDelete(buildUrl(serverConfig.getApplicationPath() + "/" + application.id()));
        try {
            httpDelete.setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());

            try (var response = httpClient.execute(httpDelete)) {
                InputStream content = response.getEntity().getContent();
                String body = new String(content.readAllBytes());
                if (response.getCode() >= 200 && response.getCode() < 300) {
                    return Wso2ApiResponse.<Application>builder()
                            .successfulCall(true)
                            .build();
                } else {
                    String errorMessage = Wso2ApiErrorHandler.extractErrorMessage(body);
                    log.warn("Deleting application with name: [{}], error: {}", application.name(), errorMessage);
                    return Wso2ApiResponse.<Application>builder()
                            .successfulCall(false)
                            .failureMessage("Failed to delete. Status: " + response.getCode())
                            .build();
                }
            }
        } catch (IOException e) {
            return handleException(e);
        }
    }

    public Wso2ApiResponse<Application> updateApplication(Application application, UpdateApplicationRequest request) {
        log.info("Updating application with id: [{}]", application.id());
        HttpPatch httpPatch = new HttpPatch(buildUrl(serverConfig.getApplicationPath() + "/" + application.id()));
        try {
            httpPatch.setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthHeader());
            httpPatch.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
            httpPatch.setEntity(new StringEntity(objectMapper.writeValueAsString(request), ContentType.APPLICATION_JSON));

            try (var response = httpClient.execute(httpPatch)) {
                InputStream content = response.getEntity().getContent();
                String body = new String(content.readAllBytes());
                if (response.getCode() >= 200 && response.getCode() < 300) {
                    return Wso2ApiResponse.<Application>builder()
                            .successfulCall(true)
                            .build();
                } else {
                    String errorMessage = Wso2ApiErrorHandler.extractErrorMessage(body);
                    log.warn("Updating application with name: [{}], error: {}", request.getName(), errorMessage);
                    return Wso2ApiResponse.<Application>builder()
                            .successfulCall(false)
                            .failureMessage("Failed to update. Status: " + response.getCode())
                            .build();
                }
            }
        } catch (IOException e) {
            return handleException(e);
        }
    }

    private String buildUrl(String path) {
        return serverConfig.getHost() + "/" + serverConfig.getServerBasePath() + "/" + path;
    }

    private String getBasicAuthHeader() {
        String credentials = serverConfig.getUsername() + ":" + serverConfig.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    private Wso2ApiResponse<Application> handleException(Exception e) {
        log.error("HTTP communication error", e);
        return Wso2ApiResponse.<Application>builder()
                .successfulCall(false)
                .failureMessage(e.getMessage())
                .build();
    }
}
