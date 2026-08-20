package cv.igrp.framework.auth.core.autoconfig;

import cv.igrp.framework.auth.core.security.IgrpAuthorizationServiceAdapter;
import cv.igrp.framework.auth.core.security.IgrpJwtAuthenticationConverter;
import cv.igrp.platform.access.client.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration entry point for {@code cv.igrp.framework.auth:core-spring-boot}.
 *
 * <p>Registered via {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * so any Spring Boot application that puts this JAR on the classpath picks up the auth wiring
 * WITHOUT needing {@code @ComponentScan("cv.igrp")}. Consumers only had to component-scan before
 * because these classes were bare {@code @Component}/{@code @Configuration} — that path still
 * works, but it made bean discovery order fragile (the converter's {@code @ConditionalOnBean}
 * check would race the adapter's registration under some scan orderings and produce a silent
 * "bean not found" — reported by App Center integrating with 0.2.0-beta.8).
 *
 * <p>{@link IgrpAuthorizationServiceAdapter} is listed FIRST in the {@code @Import} array
 * on purpose: {@link IgrpJwtAuthenticationConverter} gates its {@code JwtAuthenticationConverter}
 * bean on {@code @ConditionalOnBean(IAuthorizationServiceAdapter.class)}, and putting the
 * adapter earlier in the import order guarantees its bean definition is present when the
 * converter's condition is evaluated.
 */
@Configuration
@Import({
        IgrpAuthorizationServiceAdapter.class,
        IgrpJwtAuthenticationConverter.class,
})
public class AutoConfiguration {

    @Bean(name = "igrpApiClient")
    @ConditionalOnMissingBean
    public ApiClient apiClient(@Value("${igrp.access.api.base-url}") String baseUrl) {
        ApiClient client = new ApiClient();
        client.setBaseUrl(baseUrl);
        return client;
    }
}
