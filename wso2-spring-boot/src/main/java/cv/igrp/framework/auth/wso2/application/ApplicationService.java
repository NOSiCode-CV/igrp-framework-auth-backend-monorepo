package cv.igrp.framework.auth.wso2.application;

import cv.igrp.framework.auth.wso2.application.dto.ApplicationSearchResponse;
import cv.igrp.framework.auth.wso2.application.dto.CreateApplicationRequest;
import cv.igrp.framework.auth.wso2.application.dto.UpdateApplicationRequest;
import cv.igrp.framework.auth.wso2.config.Wso2ServerConfiguration;
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
public class ApplicationService {

    private final WebClient webClient;
    private final Wso2ServerConfiguration serverConfig;

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    public ApplicationService(WebClient.Builder webClientBuilder, Wso2ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
        this.webClient = webClientBuilder
                .baseUrl(serverConfig.getHost())
                .defaultHeaders(headers ->
                        headers.setBasicAuth(serverConfig.getUsername(), serverConfig.getPassword()))
                .build();
    }


    public Wso2ApiResponse<Application> createApplication(CreateApplicationRequest request) {
        log.info("Creating application with name {}", request.getName());
        ResponseEntity<String> response = sendCreateApplicationRequest(request);
        if (response != null && !response.getStatusCode().is2xxSuccessful()) {
            String errorMessage = Wso2ApiErrorHandler.extractErrorMessage(response);
            return Wso2ApiResponse.<Application>builder()
                    .successfulCall(false)
                    .failureMessage(errorMessage != null ? errorMessage : "No response received from WSO2 IS")
                    .build();
        }
        String location = Optional.ofNullable(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .orElse(null);

        if (location != null) {
            String applicationId = location.substring(location.lastIndexOf('/') + 1);
            Application application = new Application(applicationId, request.getName());
            return Wso2ApiResponse.<Application>builder()
                    .successfulCall(true)
                    .resourceResponse(application)
                    .build();
        }
        log.warn("Application created, but no Location header was returned.");
        return Wso2ApiResponse.<Application>builder()
                .successfulCall(true)
                .failureMessage("Application created, but no Location header returned")
                .build();
    }

    public Optional<Application> getApplicationByName(Application application) {
        log.info("Searching for application with name: {}", application.name());

        ResponseEntity<ApplicationSearchResponse> response = sendGetApplicationByNameRequest(application.name());
        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            log.error("Failed to fetch application [{}]. Status: {}", application.name(),
                    response != null ? response.getStatusCode() : "null response");
            return Optional.empty();
        }

        ApplicationSearchResponse body = response.getBody();
        if (body == null || body.getApplications() == null) {
            log.error("Empty body returned for application [{}]", application.name());
            return Optional.empty();
        }

        return body.getApplications().stream()
                .filter(app -> application.name().equals(app.getName()))
                .map(app -> new Application(app.getId(), app.getName()))
                .findFirst()
                .or(() -> {
                    log.warn("Application [{}] not found in search results", application.name());
                    return Optional.empty();
                });
    }

    public Wso2ApiResponse<Application> deleteApplication(Application application) {
        log.info("Deleting application with id {}", application.id());
        ResponseEntity<String> response = sendDeleteApplicationRequest(application);
        if (response != null && !response.getStatusCode().is2xxSuccessful()) {
            String errorMessage = Wso2ApiErrorHandler.extractErrorMessage(response);
            return Wso2ApiResponse.<Application>builder()
                    .successfulCall(false)
                    .failureMessage(errorMessage != null ? errorMessage : "No response received from WSO2 IS")
                    .build();
        }
        return Wso2ApiResponse.<Application>builder()
                .successfulCall(true)
                .build();
    }

    public Wso2ApiResponse<Application> updateApplication(Application application, UpdateApplicationRequest request) {
        log.info("Updating application with id: [{}]", application.id());
        ResponseEntity<String> response = sendUpdateApplicationRequest(application, request);
        if (response != null && !response.getStatusCode().is2xxSuccessful()) {
            String errorMessage = Wso2ApiErrorHandler.extractErrorMessage(response);
            return Wso2ApiResponse.<Application>builder()
                    .successfulCall(false)
                    .failureMessage(errorMessage != null ? errorMessage : "No response received from WSO2 IS")
                    .build();
        }
        return Wso2ApiResponse.<Application>builder()
                .successfulCall(true)
                .build();
    }

    private ResponseEntity<String> sendUpdateApplicationRequest(Application application, UpdateApplicationRequest request) {
        return webClient.patch()
                .uri("/" + serverConfig.getServerBasePath()+ "/" + serverConfig.getApplicationPath() + "/" + application.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(response -> response.toEntity(String.class))
                .block();
    }

    private ResponseEntity<ApplicationSearchResponse> sendGetApplicationByNameRequest(String appName) {
        String filterQuery = serverConfig.getApplicationFilter() + appName;
        return webClient.get()
                .uri("/" + serverConfig.getServerBasePath()+ "/" + serverConfig.getApplicationPath() + filterQuery)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(ApplicationSearchResponse.class)
                .block();
    }

    private ResponseEntity<String> sendCreateApplicationRequest(CreateApplicationRequest body) {
        return webClient.post()
                .uri("/" + serverConfig.getServerBasePath()+ "/" + serverConfig.getApplicationPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response -> response.toEntity(String.class))
                .block();
    }

    private ResponseEntity<String> sendDeleteApplicationRequest(Application application) {
        return webClient.delete()
                .uri("/" + serverConfig.getServerBasePath()+ "/" + serverConfig.getApplicationPath() + "/" + application.id())
                .exchangeToMono(response -> response.toEntity(String.class))
                .block();
    }
}
