package cv.igrp.framework.auth.wso2.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Wso2ServerConfiguration {

    private String host;
    private String username;
    private String password;
    private String serverBasePath;
    private String applicationPath;
    private String scimBasePath;
    private String userPath;
    private String groupPath;
    private String rolePath;
    private String applicationFilter = "?filter=name eq ";
    private String groupFilter = "?filter=displayName eq ";
    private String userFilter = "?filter=userName eq ";

    public Wso2ServerConfiguration(String propertiesFilePath) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(propertiesFilePath)) {
            if (input == null) {
                throw new IOException("Unable to find properties file: " + propertiesFilePath);
            }
            Properties prop = new Properties();
            prop.load(input);

            this.host = prop.getProperty("wso2.host");
            this.username = prop.getProperty("wso2.credentials.username");
            this.password = prop.getProperty("wso2.credentials.password");
            this.serverBasePath = prop.getProperty("wso2.api.server.base-path");
            this.applicationPath = prop.getProperty("wso2.api.server.application-path");
            this.scimBasePath = prop.getProperty("wso2.api.scim.base-path");
            this.userPath = prop.getProperty("wso2.api.scim.user-path");
            this.groupPath = prop.getProperty("wso2.api.scim.group-path");
            this.rolePath = prop.getProperty("wso2.api.scim.role-path");
        }
    }

    // Getters
    public String getHost() { return host; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getServerBasePath() { return serverBasePath; }
    public String getApplicationPath() { return applicationPath; }
    public String getScimBasePath() { return scimBasePath; }
    public String getUserPath() { return userPath; }
    public String getGroupPath() { return groupPath; }
    public String getRolePath() { return rolePath; }
    public String getApplicationFilter() { return applicationFilter; }
    public String getGroupFilter() { return groupFilter; }
    public String getUserFilter() { return userFilter; }
}
