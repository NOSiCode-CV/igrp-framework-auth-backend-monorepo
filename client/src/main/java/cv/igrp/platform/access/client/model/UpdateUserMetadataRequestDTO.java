package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Payload for replacing the metadata JSON of an iGRP user.
 *
 * <p>The whole map is replaced — callers that want to merge are responsible
 * for fetching the current metadata first.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateUserMetadataRequestDTO {

    @JsonProperty("metadata")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    public UpdateUserMetadataRequestDTO() {}

    public UpdateUserMetadataRequestDTO(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
