package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import cv.igrp.platform.access.client.constants.InvitationStatus;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvitationDTO {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("status")
    private InvitationStatus status;

    @JsonProperty("token")
    private String token;

    @JsonProperty("expiry")
    private String expiry;

    @JsonProperty("invitationDate")
    private String invitationDate;

    @JsonProperty("invitedBy")
    private String invitedBy;

    @JsonProperty("invitationUrl")
    private String invitationUrl;

    @JsonProperty("comments")
    private String comments;

    @JsonProperty("roles")
    private List<CodeDescriptionDTO> roles;

    @JsonProperty("department")
    private CodeDescriptionDTO department;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public String getInvitationDate() {
        return invitationDate;
    }

    public void setInvitationDate(String invitationDate) {
        this.invitationDate = invitationDate;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(String invitedBy) {
        this.invitedBy = invitedBy;
    }

    public String getInvitationUrl() {
        return invitationUrl;
    }

    public void setInvitationUrl(String invitationUrl) {
        this.invitationUrl = invitationUrl;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public List<CodeDescriptionDTO> getRoles() {
        return roles;
    }

    public void setRoles(List<CodeDescriptionDTO> roles) {
        this.roles = roles;
    }

    public CodeDescriptionDTO getDepartment() { return department; }

    public void setDepartment(CodeDescriptionDTO department) { this.department = department; }
}
