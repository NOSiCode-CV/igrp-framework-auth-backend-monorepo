package cv.igrp.framework.auth.wso2.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScimErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ScimErrorHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        try {
            ScimErrorResponse error = mapper.readValue(body, ScimErrorResponse.class);
            return error.getDetail() != null ? error.getDetail() : error.getStatus();
        } catch (Exception e) {
            log.error("Failed to parse WSO2 error body: {}", body, e);
            return body;
        }
    }
}
