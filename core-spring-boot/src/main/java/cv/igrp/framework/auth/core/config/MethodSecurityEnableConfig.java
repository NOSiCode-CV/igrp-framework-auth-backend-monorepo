package cv.igrp.framework.auth.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@AutoConfiguration
@EnableConfigurationProperties(MethodSecurityProperties.class)
@EnableMethodSecurity
@ConditionalOnProperty(
        name = "igrp.framework.auth.method-security.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MethodSecurityEnableConfig {}
