package cv.igrp.framework.auth.wso2.shared;

public class Wso2ApiResponse<T> {
    private final String failureMessage;
    private final T resourceResponse;
    private final boolean successfulCall;

    private Wso2ApiResponse(Builder<T> builder) {
        this.failureMessage = builder.failureMessage;
        this.resourceResponse = builder.resourceResponse;
        this.successfulCall = builder.successfulCall;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public T getResourceResponse() {
        return resourceResponse;
    }

    public boolean isSuccessfulCall() {
        return successfulCall;
    }

    public static final class Builder<T> {
        private String failureMessage;
        private T resourceResponse;
        private boolean successfulCall;

        private Builder() {
        }

        public Builder<T> failureMessage(String failureMessageVal) {
            failureMessage = failureMessageVal;
            return this;
        }

        public Builder<T> resourceResponse(T resourceResponseVal) {
            resourceResponse = resourceResponseVal;
            return this;
        }

        public Builder<T> successfulCall(boolean successfulCallVal) {
            successfulCall = successfulCallVal;
            return this;
        }

        public Wso2ApiResponse<T> build() {
            return new Wso2ApiResponse<>(this);
        }
    }
}
