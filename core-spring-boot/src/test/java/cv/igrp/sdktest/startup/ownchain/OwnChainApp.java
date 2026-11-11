package cv.igrp.sdktest.startup.ownchain;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Its own security chain, without the route security: every route would be open to any token. */
@SpringBootApplication
public class OwnChainApp {

    @Bean
    SecurityFilterChain api(HttpSecurity http) {
        http.authorizeHttpRequests(requests -> requests.anyRequest().authenticated());
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
