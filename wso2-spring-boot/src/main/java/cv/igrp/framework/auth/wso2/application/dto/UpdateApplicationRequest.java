package cv.igrp.framework.auth.wso2.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateApplicationRequest {
    @JsonProperty(value = "name")
    private String name;

    public UpdateApplicationRequest() {
    }

    public UpdateApplicationRequest(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }
}
