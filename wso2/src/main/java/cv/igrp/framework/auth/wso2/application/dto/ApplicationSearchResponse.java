package cv.igrp.framework.auth.wso2.application.dto;

import java.util.List;

public class ApplicationSearchResponse {
    private int totalResults;
    private List<ApplicationSummary> applications;

    public ApplicationSearchResponse() {
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public List<ApplicationSummary> getApplications() {
        return applications;
    }

    public void setApplications(List<ApplicationSummary> applications) {
        this.applications = applications;
    }

    public static class ApplicationSummary {
        private String id;
        private String name;

        public ApplicationSummary() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
