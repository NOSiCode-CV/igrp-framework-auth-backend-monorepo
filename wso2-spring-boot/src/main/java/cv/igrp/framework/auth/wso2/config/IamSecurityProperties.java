package cv.igrp.framework.auth.wso2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iam.security")
public class IamSecurityProperties {

    private boolean enabled = true;
    private final Jwt jwt = new Jwt();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public static class Jwt {
        private String authoritiesClaimName = "roles";
        private String authorityPrefix = "ROLE_";

        public String getAuthoritiesClaimName() {
            return authoritiesClaimName;
        }

        public void setAuthoritiesClaimName(String authoritiesClaimName) {
            this.authoritiesClaimName = authoritiesClaimName;
        }

        public String getAuthorityPrefix() {
            return authorityPrefix;
        }

        public void setAuthorityPrefix(String authorityPrefix) {
            this.authorityPrefix = authorityPrefix;
        }
    }
}
