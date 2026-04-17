package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidateInvitationOtpRequest {
    @JsonProperty("token")
    private String token;

    @JsonProperty("otpCode")
    private String otpCode;

    public ValidateInvitationOtpRequest() {}

    public ValidateInvitationOtpRequest(String token, String otpCode) {
        this.token = token;
        this.otpCode = otpCode;
    }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }

    public String getOtpCode() { return otpCode; }

    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

}
