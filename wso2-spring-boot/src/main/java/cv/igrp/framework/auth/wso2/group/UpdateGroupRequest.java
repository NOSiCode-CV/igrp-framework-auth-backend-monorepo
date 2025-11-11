package cv.igrp.framework.auth.wso2.group;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class UpdateGroupRequest {

    @JsonProperty("Operations")
    private final List<Operation> operations;

    public UpdateGroupRequest(String newDisplayName) {
        this.operations = List.of(new Operation("replace", Map.of("displayName", newDisplayName)));
    }

    public static class Operation {

        @JsonProperty("op")
        private final String op;

        @JsonProperty("value")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final Map<String, String> value;

        public Operation(String op, Map<String, String> value) {
            this.op = op;
            this.value = value;
        }
    }
}
