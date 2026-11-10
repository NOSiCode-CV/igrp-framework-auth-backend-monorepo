package cv.igrp.framework.auth.core.route;

import cv.igrp.platform.access.client.auth.ClientCredentialsTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * At startup, collects the service's routes (failing the startup if their keys break
 * the rules) and registers them with AM: {@code POST /api/routes/register}, with the
 * service's own {@code client_credentials} token.
 *
 * <p>A registration that does not reach AM is logged and the service starts anyway;
 * the next startup registers again.
 */
public class RouteRegistrar implements SmartInitializingSingleton {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteRegistrar.class);

    /** The request body of {@code POST /api/routes/register}. */
    public record Registration(String service, String version, List<RouteCatalog.Route> routes) {
    }

    /** The response of {@code POST /api/routes/register}. */
    public record Result(Integer created, Integer unchanged, Integer deleted, Integer renamed) {
    }

    private final ApplicationContext context;
    private final String service;
    private final String version;
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final Duration timeout;

    public RouteRegistrar(ApplicationContext context, String service, String version,
                          String baseUrl, String clientId, String clientSecret, String scope, Duration timeout) {
        this.context = context;
        this.service = service;
        this.version = version;
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.timeout = timeout;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (service.isBlank()) {
            throw new IllegalStateException(
                    "Permission keys need a service name: set igrp.security.service-name or spring.application.name");
        }
        if (version.isBlank()) {
            throw new IllegalStateException("Route registration needs the service version, which AM uses to "
                    + "gate deletes: set igrp.security.service-version (or package the jar with an "
                    + "Implementation-Version, read as spring.application.version)");
        }
        RequestMappingHandlerMapping mapping =
                context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        List<RouteCatalog.Route> routes = RouteCatalog.collect(service, mapping.getHandlerMethods());

        if (clientId.isBlank() || clientSecret.isBlank()) {
            LOGGER.warn("[Route Registration] igrp.access.m2m.client-id / client-secret not set: "
                    + "{} routes of '{}' not registered with Access Management.", routes.size(), service);
            return;
        }
        try {
            String token = new ClientCredentialsTokenProvider(baseUrl, clientId, clientSecret,
                    scope.isBlank() ? null : scope,
                    ClientCredentialsTokenProvider.DEFAULT_REFRESH_LEEWAY_SECONDS).getAccessToken();
            // Without a read timeout, an AM that accepts the call and never answers would hold the startup.
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder().connectTimeout(timeout).build());
            requestFactory.setReadTimeout(timeout);
            Result result = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build().post()
                    .uri("/api/routes/register")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Registration(service, version, routes))
                    .retrieve()
                    .body(Result.class);
            LOGGER.info("[Route Registration] {} routes of '{}' {} registered: {}",
                    routes.size(), service, version, result);
        } catch (Exception ex) {
            LOGGER.error("[Route Registration] Could not register {} routes of '{}' with Access Management; "
                    + "the next startup registers them again.", routes.size(), service, ex);
        }
    }
}
