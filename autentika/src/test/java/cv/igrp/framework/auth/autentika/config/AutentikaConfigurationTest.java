package cv.igrp.framework.auth.autentika.config;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class AutentikaConfigurationTest {
    private static final URI ENDPOINT = URI.create("https://autentika.example.test/api");
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    @Test
    void rejectsEndpointsWithCredentialsQueriesFragmentsOrUnsupportedSchemes() {
        for (String endpoint : new String[]{"https://user:pass@example.test/api",
                "https://example.test/api?key=value", "https://example.test/api#fragment", "file:///tmp/api", "/api"}) {
            URI invalidEndpoint = URI.create(endpoint);
            assertThrows(IllegalArgumentException.class, () -> new AutentikaConfiguration(
                    invalidEndpoint, ENDPOINT, () -> "token", TIMEOUT));
        }
    }

    @Test
    void rejectsInvalidTimeoutAndMissingTokenSupplier() {
        assertThrows(IllegalArgumentException.class, () -> configuration("admin", Duration.ZERO));
        Duration negativeTimeout = Duration.ofSeconds(-1);
        assertThrows(IllegalArgumentException.class, () -> configuration("admin", negativeTimeout));
        assertThrows(NullPointerException.class, () -> new AutentikaConfiguration(ENDPOINT, ENDPOINT, null, TIMEOUT));

    }

    @Test
    void configurationDoesNotExposeCredentialsInToString() {
        String description = configuration("private-username", Duration.ofSeconds(3)).toString();
        assertFalse(description.contains("private-username"));
        assertFalse(description.contains("private-password"));
    }

    private static AutentikaConfiguration configuration(String username, Duration timeout) {
        return new AutentikaConfiguration(ENDPOINT, ENDPOINT, () -> username + "private-password", timeout);
    }
}
