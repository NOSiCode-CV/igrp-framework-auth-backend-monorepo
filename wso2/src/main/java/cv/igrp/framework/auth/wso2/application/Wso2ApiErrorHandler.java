package cv.igrp.framework.auth.wso2.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import cv.igrp.framework.auth.wso2.application.dto.Wso2ApplicationErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wso2ApiErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(Wso2ApiErrorHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        try {
            Wso2ApplicationErrorResponse error = mapper.readValue(body, Wso2ApplicationErrorResponse.class);
            return error.getDescription() != null ? error.getDescription() : error.getMessage();
        } catch (Exception e) {
            log.error("Failed to parse WSO2 error body: {}", body, e);
            return body;
        }
    }
}
