package cv.igrp.framework.auth.wso2.group;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateGroupRequest {

    @JsonProperty("displayName")
    private String displayName;

    public CreateGroupRequest() {
    }

    public CreateGroupRequest(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
