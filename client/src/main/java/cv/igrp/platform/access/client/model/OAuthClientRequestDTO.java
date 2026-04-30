package cv.igrp.platform.access.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Payload for creating or updating an {@link OAuthClientDTO}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthClientRequestDTO {

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("clientName")
    private String clientName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("active")
    private boolean active = true;

    /** Owning application id (1 application → N clients). Optional. */
    @JsonProperty("applicationId")
    private Integer applicationId;

    @JsonProperty("accessTokenTtl")
    private int accessTokenTtl = 3600;

    @JsonProperty("refreshTokenTtl")
    private int refreshTokenTtl = 86400;

    @JsonProperty("authorizationCodeTtl")
    private int authorizationCodeTtl = 300;

    @JsonProperty("scopes")
    private Set<String> scopes = new LinkedHashSet<>();

    @JsonProperty("redirectUris")
    private Set<String> redirectUris = new LinkedHashSet<>();

    @JsonProperty("grantTypes")
    private Set<String> grantTypes = new LinkedHashSet<>();

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }

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
}
