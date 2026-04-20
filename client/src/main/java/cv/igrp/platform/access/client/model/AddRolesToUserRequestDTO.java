package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request body for POST /api/users/{id}/departments/{departmentCode}/roles.
 * Role codes to assign, with an optional temporary expiration timestamp.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddRolesToUserRequestDTO {

    @JsonProperty("roles")
    private List<String> roles;

    /** When set, role assignments expire at this datetime (temporary role). */
    @JsonProperty("expiresAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    public AddRolesToUserRequestDTO() {}

    public AddRolesToUserRequestDTO(List<String> roles) {
        this.roles = roles;
    }

    public AddRolesToUserRequestDTO(List<String> roles, LocalDateTime expiresAt) {
        this.roles = roles;
        this.expiresAt = expiresAt;
    }

    public List<String> getRoles() { return roles; }

    public void setRoles(List<String> roles) { this.roles = roles; }

    public LocalDateTime getExpiresAt() { return expiresAt; }

    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
