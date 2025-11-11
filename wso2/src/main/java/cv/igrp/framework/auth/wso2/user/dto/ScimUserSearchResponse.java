package cv.igrp.framework.auth.wso2.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ScimUserSearchResponse {
    @JsonProperty("totalResults")
    private int totalResults;

    @JsonProperty("startIndex")
    private int startIndex;

    @JsonProperty("itemsPerPage")
    private int itemsPerPage;

    @JsonProperty("schemas")
    private List<String> schemas;

    @JsonProperty("Resources")
    private List<Wso2UserResponse> resources;

    public ScimUserSearchResponse() {
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<String> schemas) {
        this.schemas = schemas;
    }

    public List<Wso2UserResponse> getResources() {
        return resources;
    }

    public void setResources(List<Wso2UserResponse> resources) {
        this.resources = resources;
    }
}
