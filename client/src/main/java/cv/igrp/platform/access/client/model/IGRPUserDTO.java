package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import cv.igrp.platform.access.client.constants.Status;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IGRPUserDTO {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("status")
    private Status status;

    @JsonProperty("picture")
    private String picture;

    @JsonProperty("signature")
    private String signature;

    /**
     * Optional. Maximum 13 characters when supplied; null / omitted is allowed.
     */
    @JsonProperty("nic")
    private String nic;

    /**
     * Optional. Must follow E.164 format (e.g. {@code +1234567890}) when supplied.
     * Send {@code null} (or omit the field) to indicate "no phone".
     */
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    /**
     * Mirrors {@code t_user.email_verified}. True once the user's email has
     * passed the OTP / invitation-acceptance verification flow.
     */
    @JsonProperty("emailVerified")
    private Boolean emailVerified;

    /**
     * Free-form metadata mirrored from {@code t_user.metadata} (jsonb).
     * Exposed through OAuth user-management APIs and enriched into issued
     * JWTs by the authorization server.
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    // Getters and Setters
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
