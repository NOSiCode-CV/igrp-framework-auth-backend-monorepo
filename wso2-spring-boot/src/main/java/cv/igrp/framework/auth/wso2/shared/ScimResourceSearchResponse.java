package cv.igrp.framework.auth.wso2.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ScimResourceSearchResponse {
    @JsonProperty("totalResults")
    private int totalResults;

    @JsonProperty("startIndex")
    private int startIndex;

    @JsonProperty("itemsPerPage")
    private int itemsPerPage;

    @JsonProperty("schemas")
    private List<String> schemas;

    @JsonProperty("Resources")
    private List<Resource> resources;

    public ScimResourceSearchResponse() {
    }

    public int getTotalResults() {
        return totalResults;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public List<Resource> getResources() {
        return resources;
    }


    public static class Resource {

        @JsonProperty("id")
        private String id;

        @JsonProperty("displayName")
        private String displayName;

        @JsonProperty("meta")
        private Meta meta;

        public Resource() {
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Meta getMeta() {
            return meta;
        }

        public static class Meta {

            @JsonProperty("created")
            private String created;

            @JsonProperty("lastModified")
            private String lastModified;

            @JsonProperty("location")
            private String location;

            public Meta() {
            }

            public String getCreated() {
                return created;
            }

            public String getLastModified() {
                return lastModified;
            }

            public String getLocation() {
                return location;
            }
        }
    }

}
