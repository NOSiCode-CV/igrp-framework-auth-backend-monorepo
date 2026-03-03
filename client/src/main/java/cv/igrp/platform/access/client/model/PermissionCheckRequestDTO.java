package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionCheckRequestDTO {
    @JsonProperty("resource")
    private String resource;

    @JsonProperty("action")
    private String action;

    // Constructors
    public PermissionCheckRequestDTO() {}

    public PermissionCheckRequestDTO(String resource, String action) {
        this.resource = resource;
        this.action = action;
    }

    // Getters and Setters
    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}