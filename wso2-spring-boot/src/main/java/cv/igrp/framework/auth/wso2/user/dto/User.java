package cv.igrp.framework.auth.wso2.user.dto;

import cv.igrp.framework.auth.core.model.UserIdentity;

import java.util.List;

public class User implements UserIdentity {
    private String id;
    private String userName;
    private String firstName;
    private String lastName;
    private List<String> emails;

    public User() {
    }

    private User(Builder builder) {
        id = builder.id;
        userName = builder.userName;
        firstName = builder.firstName;
        lastName = builder.lastName;
        emails = builder.emails;
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getEmail() {
        return emails.stream().findFirst().orElse(null);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String getExternalId() {
        return id;
    }

    @Override
    public boolean isEmailVerified() {
        return false;
    }

    public static final class Builder {
        private String id;
        private String userName;
        private String firstName;
        private String lastName;
        private List<String> emails;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder id(String val) {
            id = val;
            return this;
        }

        public Builder userName(String val) {
            userName = val;
            return this;
        }

        public Builder firstName(String val) {
            firstName = val;
            return this;
        }

        public Builder lastName(String val) {
            lastName = val;
            return this;
        }

        public Builder emails(List<String> val) {
            emails = val;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
