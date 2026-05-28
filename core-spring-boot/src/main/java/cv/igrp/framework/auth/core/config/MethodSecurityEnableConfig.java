package cv.igrp.framework.auth.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
@ConditionalOnProperty(
        name = "igrp.framework.auth.method-security-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MethodSecurityEnableConfig {}
