package cv.igrp.framework.auth.autentika.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("igrp.autentika")
public record AutentikaSpringProperties(
        URI registrationEndpoint,
        URI tokenEndpoint,
        String consumerKey,
        String consumerSecret,
        @DefaultValue("30s") Duration requestTimeout) {

    @Override
    public String toString() {
        return "AutentikaSpringProperties[credentials=***]";
    }
}
