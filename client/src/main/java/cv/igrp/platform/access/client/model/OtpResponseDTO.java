package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OtpResponseDTO {
    @JsonProperty("token")
    private String token;

    @JsonProperty("message")
    private String message;

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

}
