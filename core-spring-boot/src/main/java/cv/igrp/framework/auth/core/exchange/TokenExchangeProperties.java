package cv.igrp.framework.auth.core.exchange;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

@ConfigurationProperties("igrp.security.token-exchange")
public class TokenExchangeProperties {
    private URI tokenUri;
    private String clientId;
    private String clientSecret;
    private Duration timeout = Duration.ofSeconds(10);
    private Duration expirySkew = Duration.ofSeconds(30);
    private int maxEntries = 1024;
    private Map<String, Target> targets = new HashMap<>();
    public record Target(URI baseUrl, String audience, Set<String> scopes) {
        public Target {
            scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
            if (baseUrl == null || baseUrl.getHost() == null || baseUrl.getUserInfo() != null
                    || baseUrl.getQuery() != null || baseUrl.getFragment() != null
                    || !("http".equals(baseUrl.getScheme()) || "https".equals(baseUrl.getScheme()))
                    || audience == null || audience.isBlank()) throw new IllegalArgumentException("Invalid exchange target");
        }
    }
    public URI getTokenUri() { return tokenUri; }
    public void setTokenUri(URI value) { tokenUri = value; }
    public String getClientId() { return clientId; }
    public void setClientId(String value) { clientId = value; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String value) { clientSecret = value; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration value) { timeout = value; }
    public Duration getExpirySkew() { return expirySkew; }
    public void setExpirySkew(Duration value) { expirySkew = value; }
    public int getMaxEntries() { return maxEntries; }
    public void setMaxEntries(int value) { maxEntries = value; }
    public Map<String, Target> getTargets() { return targets; }
    public void setTargets(Map<String, Target> value) { targets = value; }
}
