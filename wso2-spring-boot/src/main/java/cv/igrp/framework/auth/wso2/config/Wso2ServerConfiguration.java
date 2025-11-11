package cv.igrp.framework.auth.wso2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wso2")
public class Wso2ServerConfiguration {
    @Value("${wso2.host}")
    private String host;

    @Value("${wso2.credentials.username}")
    private String username;

    @Value("${wso2.credentials.password}")
    private String password;

    @Value("${wso2.api.server.base-path}")
    private String serverBasePath;

    @Value("${wso2.api.server.application-path}")
    private String applicationPath;

    @Value("${wso2.api.scim.base-path}")
    private String scimBasePath;
    @Value("${wso2.api.scim.user-path}")
    private String userPath;

    @Value("${wso2.api.scim.group-path}")
    private String groupPath;

    @Value("${wso2.api.scim.role-path}")
    private String rolePath;
    private String applicationFilter = "?filter=name eq ";
    private String groupFilter = "?filter=displayName eq ";
    private String userFilter = "?filter=userName eq ";


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getServerBasePath() {
        return serverBasePath;
    }

    public void setServerBasePath(String serverBasePath) {
        this.serverBasePath = serverBasePath;
    }

    public String getApplicationPath() {
        return applicationPath;
    }

    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    public String getScimBasePath() {
        return scimBasePath;
    }

    public void setScimBasePath(String scimBasePath) {
        this.scimBasePath = scimBasePath;
    }

    public String getUserPath() {
        return userPath;
    }

    public void setUserPath(String userPath) {
        this.userPath = userPath;
    }

    public String getGroupPath() {
        return groupPath;
    }

    public void setGroupPath(String groupPath) {
        this.groupPath = groupPath;
    }

    public String getRolePath() {
        return rolePath;
    }

    public void setRolePath(String rolePath) {
        this.rolePath = rolePath;
    }

    public String getApplicationFilter() {
        return applicationFilter;
    }

    public void setApplicationFilter(String applicationFilter) {
        this.applicationFilter = applicationFilter;
    }

    public String getUserFilter() {
        return userFilter;
    }

    public String getGroupFilter() {
        return groupFilter;
    }
}
