package cv.igrp.framework.auth.wso2.role.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AddUserToRoleRequest {

    @JsonProperty("Operations")
    private final List<Operation> operations;

    public AddUserToRoleRequest(String userId) {
        this.operations = List.of(new Operation("add", List.of(new UserValue(userId))));
    }

    public static class Operation {

        @JsonProperty("op")
        private final String op;

        @JsonProperty("path")
        private final String path = "users";

        @JsonProperty("value")
        private final List<UserValue> value;

        public Operation(String op, List<UserValue> value) {
            this.op = op;
            this.value = value;
        }
    }

    public static class UserValue {
        @JsonProperty("value")
        private final String value;

        public UserValue(String value) {
            this.value = value;
        }
    }
}
