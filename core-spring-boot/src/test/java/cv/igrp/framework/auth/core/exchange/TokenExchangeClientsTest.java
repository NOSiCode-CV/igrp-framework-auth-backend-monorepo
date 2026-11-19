package cv.igrp.framework.auth.core.exchange;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class TokenExchangeClientsTest {
    @Test void twoOutgoingCallsMakeOneExchangeAndSendTheSameBearer() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var exchanges = new AtomicInteger();
        var calls = new AtomicInteger();
        server.createContext("/token", request -> {
            exchanges.incrementAndGet();
            byte[] body = "{\"access_token\":\"for-b\",\"token_type\":\"Bearer\",\"expires_in\":300}"
                    .getBytes(StandardCharsets.UTF_8);
            request.sendResponseHeaders(200, body.length);
            request.getResponseBody().write(body);
            request.close();
        });
        server.createContext("/invoices", request -> {
            if ("Bearer for-b".equals(request.getRequestHeaders().getFirst("Authorization"))) calls.incrementAndGet();
            request.sendResponseHeaders(204, -1);
            request.close();
        });
        server.start();
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            Clock clock = Clock.systemUTC();
            var transport = new TokenExchangeHttpClient(base.resolve("/token"), "a", "secret", Duration.ofSeconds(2), clock);
            var cache = new ExchangedTokenCache(transport, clock, Duration.ofSeconds(10), 10);
            var factory = new TokenExchangeClients(cache, Map.of("billing",
                    new TokenExchangeProperties.Target(base, "b", Set.of("read"))), Duration.ofSeconds(2));
            var subject = Jwt.withTokenValue("subject").header("alg", "RS256").subject("user")
                    .expiresAt(Instant.now().plusSeconds(600)).build();
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(subject, List.of()));
            var client = factory.client("billing");
            client.get().uri("/invoices").retrieve().toBodilessEntity();
            client.get().uri("/invoices").retrieve().toBodilessEntity();
            assertEquals(1, exchanges.get());
            assertEquals(2, calls.get());
            assertThrows(IllegalArgumentException.class, () ->
                    client.get().uri("http://different.invalid/invoices").retrieve().toBodilessEntity());
            assertEquals(1, exchanges.get());
        } finally {
            SecurityContextHolder.clearContext();
            server.stop(0);
        }
    }
}
