package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionInitRequestDTO {

    @JsonProperty("clientIp")
    private String clientIp;

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("userAgent")
    private String userAgent;

    public SessionInitRequestDTO() {}

    public SessionInitRequestDTO(String clientIp, String deviceId, String userAgent) {
        this.clientIp = clientIp;
        this.deviceId = deviceId;
        this.userAgent = userAgent;
    }

    // Getters and Setters
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
