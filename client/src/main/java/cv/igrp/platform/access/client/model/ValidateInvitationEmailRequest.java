package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidateInvitationEmailRequest {
    @JsonProperty("token")
    private String token;

    @JsonProperty("email")
    private String email;

    public ValidateInvitationEmailRequest() {}

    public ValidateInvitationEmailRequest(String token, String email) {
        this.token = token;
        this.email = email;
    }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

}
