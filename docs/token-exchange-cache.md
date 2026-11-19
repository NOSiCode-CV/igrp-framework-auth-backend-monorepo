# Reuse exchanged tokens on outgoing calls

Enable the feature in the calling service A:

```yaml
igrp:
  security:
    token-exchange:
      enabled: true
      token-uri: https://am.example/oauth2/token
      client-id: service-a
      client-secret: ${SERVICE_A_SECRET}
      max-entries: 1024
      expiry-skew: 30s
      timeout: 10s
      targets:
        billing:
          base-url: https://service-b.example
          audience: service-b
          scopes: [invoices.read]
```

Inject `TokenExchangeClients` and create a target-specific `RestClient` once:

```java
BillingGateway(TokenExchangeClients clients) {
    this.client = clients.client("billing");
}

// Inside an authenticated user request:
client.get().uri("/invoices/42").retrieve().toBodilessEntity();
```

The first outgoing call exchanges the authenticated user's JWT. Repeated calls with the same input token, target and scopes reuse the exchanged bearer token. No exchange occurs at login, client creation, or when there is no outgoing call. No actor token is required for the default flow; AM identifies A through client authentication. An overload accepts a `Supplier<Jwt>` when A explicitly manages an actor token.

The shared, process-local cache is bounded (1024 entries by default). Keys use SHA-256 fingerprints of the subject/actor tokens, audience and sorted scopes. They therefore isolate different users, sessions, roles, issuers, actor tokens and token rotations. A refreshed input token intentionally causes a new exchange even for the same user and target. The cache does not share tokens across application replicas.

Expiry is bounded by `expires_in`, the subject and optional actor expiry. Tokens within the 30-second safety margin are refreshed on the next call. Concurrent calls in the same context share one successful exchange. Failed exchanges are not cached. A downstream 401 evicts the rejected token; requests are not automatically replayed, including POSTs. `ExchangedTokenCache.clear()` is available for application-driven invalidation. Remote revocation is not detected by this local cache before expiry/401.

Only calls made through these clients get the exchange behavior; arbitrary existing RestClients/WebClients are not intercepted. Outgoing URLs must remain inside the configured target origin/path, and redirects are disabled. This target mapping describes where to call; it is not a client-to-audience allowlist in AM.

The caller must already be authenticated as a JwtAuthenticationToken. Background work must propagate the authenticated security context explicitly; autonomous work should continue using client_credentials instead. This feature is disabled unless explicitly enabled and does not replace the existing M2M client.

Tests cover reuse, expiry, context isolation, capacity, eviction, concurrency and two HTTP calls producing one token exchange. Validation command: `mvn -pl core-spring-boot -am "-Dtest=*,!**/harness/*" -Dsurefire.failIfNoSpecifiedTests=false test`. The Docker integration harness is excluded at the user's request. Publish/install the updated starter and update the consuming service to activate it.
