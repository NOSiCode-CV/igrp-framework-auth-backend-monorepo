# Autentika Spring Boot

Spring Boot 4 auto-configuration for the Autentika adapter. Requires Java 26 and includes the `autentika` library.

## Dependency

```xml
<dependency>
    <groupId>cv.igrp.framework.auth</groupId>
    <artifactId>autentika-spring-boot</artifactId>
    <version>0.3.0-beta.1-SNAPSHOT</version>
</dependency>
```

## Configuration

Add to `application.yml` and supply credentials through environment variables:

```yaml
igrp:
  autentika:
    enabled: true
    registration-endpoint: https://gateway-stage-pdex.gov.cv/identity/register/1.0.0/register
    token-endpoint: https://gateway-stage-pdex.gov.cv/token
    consumer-key: ${AUTENTIKA_CONSUMER_KEY}
    consumer-secret: ${AUTENTIKA_CONSUMER_SECRET}
    request-timeout: 30s
```

The URLs above target staging. Replace them for other environments. The timeout defaults to 30 seconds; auto-configuration is disabled unless `enabled` is `true`.

## Injection

Inject `IAdapter` into a Spring-managed service using constructor injection (`@Autowired` is unnecessary for a single constructor):

```java
import cv.igrp.framework.auth.core.adapter.IAdapter;
import org.springframework.stereotype.Service;

@Service
public class ClientService {
    private final IAdapter adapter;

    public ClientService(IAdapter adapter) {
        this.adapter = adapter;
    }
}
```

No component scanning or manual bean declaration is needed. Gateway tokens are obtained lazily, cached and renewed automatically. An existing `IAdapter` bean takes precedence; a custom `AutentikaConfiguration` bean can provide your own token supplier.

See the [Autentika README](../autentika/README.md#supported-operations) for supported operations and integration limitations.

## Build and test

Run from the repository root:

```sh
mvn -pl autentika-spring-boot -am clean install
```

Builds both Autentika JARs and runs unit and Spring context tests without contacting the gateway.
