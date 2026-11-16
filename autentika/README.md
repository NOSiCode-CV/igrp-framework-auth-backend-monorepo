# Autentika

Java 26 library implementing iGRP's `IAdapter` for Autentika, based on the WSO2 Identity Server 5.11 OAuth DCR API.

## Spring Boot usage

For Spring Boot 4 / Java 26 applications, add this dependency (it includes `autentika`):

```xml
<dependency>
    <groupId>cv.igrp.framework.auth</groupId>
    <artifactId>autentika-spring-boot</artifactId>
    <version>0.3.0-beta.1-SNAPSHOT</version>
</dependency>
```

Configure `application.yml`:

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

Inject `IAdapter` into your Spring service; no component scanning or manual bean declaration is needed:

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

Tokens are fetched on the first management request, cached and renewed automatically. An existing `IAdapter` bean takes precedence. A custom `AutentikaConfiguration` bean can supply your own token provider.

## Plain Java usage

Outside Spring Boot, depend on `autentika` and construct the adapter directly:

```java
import cv.igrp.framework.auth.autentika.adapter.AutentikaAdapter;
import cv.igrp.framework.auth.autentika.config.AutentikaConfiguration;
import cv.igrp.framework.auth.core.model.ClientRegistration;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;

var config = AutentikaConfiguration.withClientCredentials(
    URI.create(System.getenv("AUTENTIKA_REGISTRATION_ENDPOINT")),
    URI.create(System.getenv("AUTENTIKA_TOKEN_ENDPOINT")),
    System.getenv("AUTENTIKA_CONSUMER_KEY"),
    System.getenv("AUTENTIKA_CONSUMER_SECRET"),
    Duration.ofSeconds(30)
);

var adapter = new AutentikaAdapter(config);
var client = adapter.createClient(new ClientRegistration(
    null, "my-service", Set.of("client_credentials"), List.of()
));
```

The library obtains, caches and renews gateway tokens automatically. Supply the full registration URL ending in `/register` and the token URL ending in `/token`.

## Supported operations

- Create, update and delete OAuth clients; validate client credentials.
- Create applications by name, rename them and delete them through DCR. Name-only creation uses `client_credentials`; use `createClient` to receive credentials.
- Application methods use `client_name`; client methods use `client_id`. Departments are not mapped.

Secret rotation and other mutations are unsupported. Remaining queries are placeholders. The staging gateway has confirmed token issuance and registration; lookup/update/delete routes returned 404 or 405 during testing, so full lifecycle integration is not yet verified.

## Build and tests

Run from the repository root:

```sh
mvn -pl autentika-spring-boot -am clean install
mvn -pl autentika -am -Pautentika-integration "-Dit.test=AutentikaAdapterIntegrationTest#gatewayTokenCanBeIssued" verify
```

The first command builds the JAR and runs unit tests with mocks. The second tests real gateway token issuance using the environment variables above. Full integration tests (`-Pautentika-integration verify`) create temporary clients and require working management routes for cleanup.
