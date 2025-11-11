package cv.igrp.framework.auth.core.autoconfig;

import cv.igrp.platform.access.client.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoConfiguration {

    @Bean(name = "igrpApiClient")
    @ConditionalOnMissingBean
    public ApiClient apiClient(@Value("${igrp.access.api.base-url}") String baseUrl) {
        ApiClient client = new ApiClient();
        client.setBaseUrl(baseUrl);
        return client;
    }
}
