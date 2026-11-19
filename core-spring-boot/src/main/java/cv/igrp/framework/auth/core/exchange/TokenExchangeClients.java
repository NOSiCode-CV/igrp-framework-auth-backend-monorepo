package cv.igrp.framework.auth.core.exchange;

import cv.igrp.framework.auth.core.audit.CorrelationIds;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Target-specific clients exchange lazily on outgoing calls; all clients share the same cache. */
public final class TokenExchangeClients {
    private final ExchangedTokenCache cache;
    private final Map<String, TokenExchangeProperties.Target> targets;
    private final Duration timeout;

    public TokenExchangeClients(ExchangedTokenCache cache, Map<String, TokenExchangeProperties.Target> targets,
            Duration timeout) {
        this.cache = cache;
        this.targets = Map.copyOf(targets);
        this.timeout = timeout;
    }
    public RestClient client(String name) { return client(name, () -> null); }

    /** Optional actor supplier for a caller that already manages its own service account token. */
    public RestClient client(String name, Supplier<Jwt> actorSupplier) {
        var target = targets.get(name);
        if (target == null) throw new IllegalArgumentException("Unknown token exchange destination: " + name);
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(timeout);
        return RestClient.builder().baseUrl(target.baseUrl().toString()).requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    requireTarget(request.getURI(), target.baseUrl());
                    var authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()) {
                        throw new IllegalStateException("Token exchange requires an authenticated user JWT");
                    }
                    Jwt subject = jwt.getToken();
                    Jwt actor = actorSupplier.get();
                    String token = cache.get(subject, actor, target.audience(), target.scopes());
                    request.getHeaders().setBearerAuth(token);
                    CorrelationIds.outgoingHeaders().forEach(request.getHeaders()::set);
                    var response = execution.execute(request, body);
                    if (response.getStatusCode().value() == 401) {
                        cache.invalidate(subject, actor, target.audience(), target.scopes(), token);
                    }
                    // Do not retry automatically: the outgoing request may be non-idempotent.
                    return response;
                }).build();
    }

    private static void requireTarget(URI request, URI base) {
        String path = request.getPath();
        String prefix = base.getPath();
        if (prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);
        boolean traversal = java.util.Arrays.asList(path.split("/")).contains("..");
        if (!Objects.equals(request.getScheme(), base.getScheme())
                || !Objects.equals(request.getHost(), base.getHost()) || request.getPort() != base.getPort()
                || request.getUserInfo() != null || traversal
                || !(prefix.isEmpty() || path.equals(prefix) || path.startsWith(prefix + "/"))) {
            throw new IllegalArgumentException("Request is outside the configured token exchange destination");
        }
    }
}
