package cv.igrp.framework.auth.core.autoconfig;

import cv.igrp.framework.auth.core.audit.CorrelationIds;
import cv.igrp.framework.auth.core.security.AuthenticationHelper;
import cv.igrp.framework.auth.core.security.IAuthorizationServiceAdapter;
import cv.igrp.framework.auth.core.security.IgrpAuthorizationService;
import cv.igrp.framework.auth.core.security.IgrpAuthorizationServiceAdapter;
import cv.igrp.framework.auth.core.route.RouteCatalog;
import cv.igrp.framework.auth.core.route.RouteRegistrar;
import cv.igrp.platform.access.client.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * Auto-configuration entry point for {@code cv.igrp.framework.auth:core-spring-boot}.
 *
 * <p>Registered via {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * so any Spring Boot application that puts this JAR on the classpath gets the whole auth
 * wiring WITHOUT {@code @ComponentScan("cv.igrp")}. The classes it creates carry no
 * stereotypes any more, so a scan finds nothing extra. Applications that still scan
 * {@code cv.igrp} keep working: Spring processes this class once, whichever way it arrives.
 *
 * <p>Bean names are part of the contract: {@code @PreAuthorize("@igrpAuthorization...")}
 * resolves {@code igrpAuthorization} by name.
 */
@org.springframework.boot.autoconfigure.AutoConfiguration
public class AutoConfiguration {

    @Bean(name = "igrpApiClient")
    @ConditionalOnMissingBean
    public ApiClient apiClient(@Value("${igrp.access.api.base-url}") String baseUrl) {
        ApiClient client = new ApiClient();
        client.setBaseUrl(baseUrl);
        // v3-24: calls to AM carry the caller's traceparent and X-Correlation-ID.
        client.setDefaultHeaders(CorrelationIds::outgoingHeaders);
        return client;
    }

    @Bean
    @ConditionalOnMissingBean(IAuthorizationServiceAdapter.class)
    @ConditionalOnProperty(
            name = "igrp.authorization.service.adapter",
            havingValue = "igrp",
            matchIfMissing = true
    )
    public IgrpAuthorizationServiceAdapter igrpAuthorizationServiceAdapter() {
        return new IgrpAuthorizationServiceAdapter();
    }

    @Bean(name = "igrpAuthorization")
    @ConditionalOnMissingBean(name = "igrpAuthorization")
    public IgrpAuthorizationService igrpAuthorization() {
        return new IgrpAuthorizationService();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationHelper authenticationHelper() {
        return new AuthenticationHelper();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationSyncRunner authorizationSyncRunner(ApiClient igrpApiClient) {
        return new AuthorizationSyncRunner(igrpApiClient);
    }

    /**
     * Gives every route a permission key and registers the routes with AM at startup.
     * The startup fails when a key breaks the rules (see {@link RouteCatalog}).
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public RouteRegistrar routeRegistrar(
            ApplicationContext context,
            @Value("${igrp.security.service-name:${spring.application.name:}}") String service,
            @Value("${igrp.security.service-version:${spring.application.version:}}") String version,
            @Value("${igrp.access.api.base-url}") String baseUrl,
            @Value("${igrp.access.m2m.client-id:}") String clientId,
            @Value("${igrp.access.m2m.client-secret:}") String clientSecret,
            @Value("${igrp.access.m2m.scope:}") String scope,
            @Value("${igrp.security.registration-timeout:10s}") Duration timeout) {
        return new RouteRegistrar(context, service, version,
                new RouteRegistrar.AccessManagement(baseUrl, clientId, clientSecret, scope, timeout));
    }
}
