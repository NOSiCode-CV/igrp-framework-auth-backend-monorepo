package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Request body for {@link cv.igrp.platform.access.client.api.ServiceAccountsApi}
 * create and update operations.
 *
 * <p>{@link #oauthClientId} is REQUIRED — create the OAuth2 client
 * separately first (with {@code grantTypes} including
 * {@code client_credentials}) and pass its id here.
 *
 * <p>{@link #roleIds} and {@link #permissionIds} use REPLACEMENT semantics
 * on {@code PUT}: ids omitted from the request are unassigned. To add
 * one role, the caller must send the current set plus the new id.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceAccountRequestDTO {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("active")
    private boolean active = true;

    @JsonProperty("oauthClientId")
    private String oauthClientId;

    @JsonProperty("applicationId")
    private Integer applicationId;

    @JsonProperty("roleIds")
    private Set<Integer> roleIds = new LinkedHashSet<>();

    /**
     * Permission ids granted DIRECTLY on the service account, bypassing the
     * role layer. The effective permission set at token-issue time is the
     * union of these direct grants and the permissions inherited from
     * {@link #roleIds}.
     */
    @JsonProperty("permissionIds")
    private Set<Integer> permissionIds = new LinkedHashSet<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getOauthClientId() { return oauthClientId; }
    public void setOauthClientId(String oauthClientId) { this.oauthClientId = oauthClientId; }

    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }

    public Set<Integer> getRoleIds() { return roleIds; }
    public void setRoleIds(Set<Integer> roleIds) { this.roleIds = roleIds; }

    public Set<Integer> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(Set<Integer> permissionIds) { this.permissionIds = permissionIds; }
}
