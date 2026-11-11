package cv.igrp.framework.auth.core.route;

import cv.igrp.framework.auth.core.audit.Audit;
import cv.igrp.framework.auth.core.audit.CorrelationIds;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.security.autoconfigure.web.servlet.ConditionalOnDefaultWebSecurity;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.HandlerMapping;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Closed by default (v3-07): every route of the service needs its permission key, decided
 * per request by {@link RouteDecisionFilter}. Runs before Spring Boot's own web security,
 * so the starter's chain replaces Boot's default one; a service with its own chain applies
 * {@link RouteSecurity} to it, or does not start.
 */
@org.springframework.boot.autoconfigure.AutoConfiguration(
        beforeName = {
                "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
                "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration",
                "org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration"},
        afterName = {
                "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
                "org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration",
                "cv.igrp.framework.auth.core.audit.AuditAutoConfiguration"})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(SecurityFilterChain.class)
public class RouteSecurityAutoConfiguration {

    @Bean
    public RouteSecurity igrpRouteSecurity(
            ApplicationContext context, Environment environment,
            @Value("${igrp.security.service-name:${spring.application.name:}}") String service,
            @Value("${igrp.security.application-code:}") String applicationCode,
            @Value("${igrp.access.api.base-url}") String amBaseUrl,
            @Value("${igrp.security.permission-fetch-timeout:5s}") Duration fetchTimeout,
            ObjectProvider<StringRedisTemplate> redis,
            ObjectProvider<Audit> audit,
            ObjectProvider<PlatformTransactionManager> transactions) {
        if (applicationCode.isBlank()) {
            throw new IllegalStateException("igrp.security.application-code (IGRP_SECURITY_APPLICATION_CODE) is "
                    + "required: it names the application whose permission set this service checks");
        }
        List<String> publicPaths = Binder.get(environment)
                .bind("igrp.security.public-paths", Bindable.listOf(String.class)).orElse(List.of());

        PermissionLookup lookup = new PermissionLookup(redis.getIfAvailable(), amClient(amBaseUrl, fetchTimeout),
                applicationCode);
        return new RouteSecurity(() -> new RouteResolver(service, handlerMappings(context), publicPaths),
                lookup, accessAudit(audit.getIfAvailable(), transactions.getIfAvailable()));
    }

    /** Replaces Spring Boot's default chain: the resource server, with the route security. */
    @Bean
    @ConditionalOnDefaultWebSecurity
    public SecurityFilterChain igrpSecurityFilterChain(HttpSecurity http, RouteSecurity routeSecurity) {
        routeSecurity.apply(http);
        http.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /** A service's own chain without the route security would leave its routes unchecked: refuse to start. */
    @Bean
    public SmartInitializingSingleton igrpRouteSecurityCheck(ObjectProvider<SecurityFilterChain> chains) {
        return () -> {
            boolean applied = chains.orderedStream()
                    .anyMatch(chain -> chain.getFilters().stream().anyMatch(RouteDecisionFilter.class::isInstance));
            if (!applied) {
                throw new IllegalStateException("No SecurityFilterChain applies the iGRP route security, so the "
                        + "service's routes would not be checked. Call routeSecurity.apply(http) in your chain "
                        + "(RouteSecurity is a bean of the starter).");
            }
        };
    }

    private static RestClient amClient(String baseUrl, Duration timeout) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build());
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    CorrelationIds.outgoingHeaders().forEach(request.getHeaders()::set);
                    return execution.execute(request, body);
                })
                .build();
    }

    private static AccessAudit accessAudit(Audit audit, PlatformTransactionManager transactions) {
        if (audit == null || transactions == null) {
            return new AccessAudit(null, null);
        }
        TransactionTemplate ownTransaction = new TransactionTemplate(transactions);
        ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new AccessAudit(audit, ownTransaction);
    }

    private static List<HandlerMapping> handlerMappings(ApplicationContext context) {
        List<HandlerMapping> mappings = new ArrayList<>(context.getBeansOfType(HandlerMapping.class).values());
        AnnotationAwareOrderComparator.sort(mappings);
        return mappings;
    }
}
