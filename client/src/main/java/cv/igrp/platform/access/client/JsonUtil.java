package cv.igrp.platform.access.client;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * The SDK's private JSON mapper.
 *
 * <p>Jackson 3 with Jackson 2 defaults, so the bytes sent to the Access
 * Management API are the same as from older SDK releases
 * ({@code WireFormatBaselineTest} pins them). Dates stay ISO-8601 strings.
 *
 * <p>Jackson 3 exceptions are unchecked; they are rethrown as {@link IOException}
 * so a malformed body keeps taking the {@code catch (IOException)} paths that
 * turn it into an {@link ApiException}.
 */
public class JsonUtil {
    private static final JsonMapper objectMapper = JsonMapper.builder()
            .configureForJackson2()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    public static String serialize(Object object) throws IOException {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new IOException(e);
        }
    }

    public static <T> T deserialize(String json, Class<T> valueType) throws IOException {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JacksonException e) {
            throw new IOException(e);
        }
    }

    public static <T> T deserialize(String json, TypeReference<T> typeRef) throws IOException {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JacksonException e) {
            throw new IOException(e);
        }
    }
}
