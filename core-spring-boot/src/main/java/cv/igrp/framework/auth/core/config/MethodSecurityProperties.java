package cv.igrp.framework.auth.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "igrp.framework.auth.method-security")
public record MethodSecurityProperties(
        boolean enabled
) {
}