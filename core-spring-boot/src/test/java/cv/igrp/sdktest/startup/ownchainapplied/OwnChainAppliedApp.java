package cv.igrp.sdktest.startup.ownchainapplied;

import cv.igrp.framework.auth.core.route.RouteSecurity;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Its own security chain, with the route security applied the documented way. */
@SpringBootApplication
public class OwnChainAppliedApp {

    @Bean
    SecurityFilterChain api(HttpSecurity http, RouteSecurity routeSecurity) {
        routeSecurity.apply(http);
        http.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @RestController
    static class Routes {

        @GetMapping("/faturas")
        String list() {
            return "[]";
        }
    }
}
