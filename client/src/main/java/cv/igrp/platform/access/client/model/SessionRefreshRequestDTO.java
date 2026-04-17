package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionRefreshRequestDTO {

    @JsonProperty("extensionSeconds")
    private Integer extensionSeconds;

    public SessionRefreshRequestDTO() {}

    public SessionRefreshRequestDTO(Integer extensionSeconds) {
        this.extensionSeconds = extensionSeconds;
    }

    // Getters and Setters
    public Integer getExtensionSeconds() { return extensionSeconds; }
    public void setExtensionSeconds(Integer extensionSeconds) { this.extensionSeconds = extensionSeconds; }
}
