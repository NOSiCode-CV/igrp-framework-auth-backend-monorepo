package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service account persisted on the iGRP authorization server.
 *
 * <p>A service account is a 1:1 wrapper around an OAuth2 client used for
 * machine-to-machine access. It carries its own {@code roleIds} and,
 * optionally, {@code permissionIds} granted DIRECTLY on the account
 * (bypassing the role layer). The effective permission set at token-issue
 * time is the union of role-inherited and directly-granted permissions.
 *
 * <p>Secrets are never surfaced on service-account endpoints — they live
 * on the linked {@link OAuthClientDTO} and are only visible on that
 * client's creation response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceAccountDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("active")
    private boolean active;

    /** UUID of the linked OAuth2 client (1:1). */
    @JsonProperty("oauthClientId")
    private String oauthClientId;

    /** Denormalised for convenience — same as {@code oauthClient.clientId}. */
    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("applicationId")
    private Integer applicationId;

    @JsonProperty("applicationCode")
    private String applicationCode;

    /**
     * Roles held, each as an id/code/department triple, ordered by id.
     *
     * <p>Use this rather than {@link #roleIds} + {@link #roleCodes}: those are two
     * independent sets and item <i>n</i> of one is NOT item <i>n</i> of the other.
     */
    @JsonProperty("roles")
    private List<ServiceAccountRoleDTO> roles = new ArrayList<>();

    /**
     * Permissions granted DIRECTLY on the account (bypassing the role layer), each
     * as an id/name pair, ordered by id. Use this rather than
     * {@link #permissionIds} + {@link #permissionNames}, for the same reason.
     */
    @JsonProperty("permissions")
    private List<ServiceAccountPermissionDTO> permissions = new ArrayList<>();

    /** @deprecated cannot be paired with {@link #roleCodes}; use {@link #roles}. */
    @Deprecated
    @JsonProperty("roleIds")
    private Set<Integer> roleIds = new LinkedHashSet<>();

    /** @deprecated cannot be paired with {@link #roleIds}; use {@link #roles}. */
    @Deprecated
    @JsonProperty("roleCodes")
    private Set<String> roleCodes = new LinkedHashSet<>();

    /** @deprecated cannot be paired with {@link #permissionNames}; use {@link #permissions}. */
    @Deprecated
    @JsonProperty("permissionIds")
    private Set<Integer> permissionIds = new LinkedHashSet<>();

    /** @deprecated cannot be paired with {@link #permissionIds}; use {@link #permissions}. */
    @Deprecated
    @JsonProperty("permissionNames")
    private Set<String> permissionNames = new LinkedHashSet<>();

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getOauthClientId() { return oauthClientId; }
    public void setOauthClientId(String oauthClientId) { this.oauthClientId = oauthClientId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }

    public String getApplicationCode() { return applicationCode; }
    public void setApplicationCode(String applicationCode) { this.applicationCode = applicationCode; }

    public List<ServiceAccountRoleDTO> getRoles() { return roles; }
    public void setRoles(List<ServiceAccountRoleDTO> roles) { this.roles = roles; }

    public List<ServiceAccountPermissionDTO> getPermissions() { return permissions; }
    public void setPermissions(List<ServiceAccountPermissionDTO> permissions) { this.permissions = permissions; }

    public Set<Integer> getRoleIds() { return roleIds; }
    public void setRoleIds(Set<Integer> roleIds) { this.roleIds = roleIds; }

    public Set<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(Set<String> roleCodes) { this.roleCodes = roleCodes; }

    public Set<Integer> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(Set<Integer> permissionIds) { this.permissionIds = permissionIds; }

    public Set<String> getPermissionNames() { return permissionNames; }
    public void setPermissionNames(Set<String> permissionNames) { this.permissionNames = permissionNames; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
