package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * OAuth2 client persisted in the iGRP authorization server.
 *
 * <p>The raw {@link #clientSecret} is populated only on creation responses
 * so the operator can hand the secret off; subsequent reads do not expose it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthClientDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("clientSecret")
    private String clientSecret;

    @JsonProperty("clientName")
    private String clientName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("applicationId")
    private Integer applicationId;

    @JsonProperty("applicationCode")
    private String applicationCode;

    @JsonProperty("accessTokenTtl")
    private int accessTokenTtl;

    @JsonProperty("refreshTokenTtl")
    private int refreshTokenTtl;

    @JsonProperty("authorizationCodeTtl")
    private int authorizationCodeTtl;

    @JsonProperty("scopes")
    private Set<String> scopes = new LinkedHashSet<>();

    @JsonProperty("redirectUris")
    private Set<String> redirectUris = new LinkedHashSet<>();

    @JsonProperty("grantTypes")
    private Set<String> grantTypes = new LinkedHashSet<>();

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }

    public String getApplicationCode() { return applicationCode; }
    public void setApplicationCode(String applicationCode) { this.applicationCode = applicationCode; }

    public int getAccessTokenTtl() { return accessTokenTtl; }
    public void setAccessTokenTtl(int accessTokenTtl) { this.accessTokenTtl = accessTokenTtl; }

    public int getRefreshTokenTtl() { return refreshTokenTtl; }
    public void setRefreshTokenTtl(int refreshTokenTtl) { this.refreshTokenTtl = refreshTokenTtl; }

    public int getAuthorizationCodeTtl() { return authorizationCodeTtl; }
    public void setAuthorizationCodeTtl(int authorizationCodeTtl) { this.authorizationCodeTtl = authorizationCodeTtl; }

    public Set<String> getScopes() { return scopes; }
    public void setScopes(Set<String> scopes) { this.scopes = scopes; }

    public Set<String> getRedirectUris() { return redirectUris; }
    public void setRedirectUris(Set<String> redirectUris) { this.redirectUris = redirectUris; }

    public Set<String> getGrantTypes() { return grantTypes; }
    public void setGrantTypes(Set<String> grantTypes) { this.grantTypes = grantTypes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
