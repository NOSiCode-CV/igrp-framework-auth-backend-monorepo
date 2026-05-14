package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Free-form user metadata persisted on {@code t_user.metadata} and surfaced
 * through OAuth2 identity-related APIs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserMetadataDTO {

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("metadata")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
