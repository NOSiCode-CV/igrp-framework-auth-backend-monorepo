package cv.igrp.framework.auth.autentika.config;

import cv.igrp.framework.auth.autentika.client.AutentikaTokenProvider;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Gateway DCR and token endpoints. The supplier returns a current gateway bearer
 * token; OAuth client authentication uses its own client ID and secret instead.
 */
public record AutentikaConfiguration(
		URI registrationEndpoint,
		URI tokenEndpoint,
		Supplier<String> gatewayAccessToken,
		Duration requestTimeout
) {

    /** Creates a configuration that automatically obtains and renews gateway tokens. */
    public static AutentikaConfiguration withClientCredentials(
            URI registrationEndpoint,
			URI tokenEndpoint,
			String consumerKey,
			String consumerSecret,
            Duration requestTimeout) {
        return new AutentikaConfiguration(registrationEndpoint, tokenEndpoint,
                new AutentikaTokenProvider(tokenEndpoint, consumerKey, consumerSecret, requestTimeout), requestTimeout);
    }

    public AutentikaConfiguration {
        validateEndpoint(registrationEndpoint);
        validateEndpoint(tokenEndpoint);
        Objects.requireNonNull(gatewayAccessToken, "gatewayAccessToken");
        Objects.requireNonNull(requestTimeout, "requestTimeout");
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
    }

    private static void validateEndpoint(URI uri) {
        Objects.requireNonNull(uri, "endpoint");
        if (uri.getHost() == null || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Endpoints must be HTTP(S) URLs without credentials, query or fragment");
        }
    }

    @Override
    public String toString() {
        return "AutentikaConfiguration[registrationEndpoint=" + registrationEndpoint
                + ", tokenEndpoint=" + tokenEndpoint + ", gatewayAccessToken=***, requestTimeout=" + requestTimeout + "]";
    }
}
