package cv.igrp.framework.auth.wso2.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateApplicationRequest {
    @JsonProperty(value = "name")
    private String name;

    public CreateApplicationRequest() {
    }

    public CreateApplicationRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
