package cv.igrp.framework.auth.autentika.config;

import cv.igrp.framework.auth.autentika.adapter.AutentikaAdapter;
import cv.igrp.framework.auth.core.adapter.IAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(beforeName = "cv.igrp.framework.auth.keycloak.config.AutoConfiguration")
@ConditionalOnProperty(prefix = "igrp.autentika", name = "enabled", havingValue = "true")
@ConditionalOnMissingBean(IAdapter.class)
@EnableConfigurationProperties(AutentikaSpringProperties.class)
public class AutentikaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AutentikaConfiguration autentikaConfiguration(AutentikaSpringProperties properties) {
        return AutentikaConfiguration.withClientCredentials(
                properties.registrationEndpoint(), properties.tokenEndpoint(),
                properties.consumerKey(), properties.consumerSecret(), properties.requestTimeout());
    }

    @Bean
    public AutentikaAdapter autentikaAdapter(AutentikaConfiguration configuration) {
        return new AutentikaAdapter(configuration);
    }
}
