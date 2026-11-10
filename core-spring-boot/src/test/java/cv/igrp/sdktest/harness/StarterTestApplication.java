package cv.igrp.sdktest.harness;

import cv.igrp.framework.auth.core.route.Permission;
import cv.igrp.framework.auth.core.route.Public;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A service that uses the starter the way a real one does: it lists no iGRP
 * configuration and scans only its own package, so everything the starter
 * contributes arrives through {@code AutoConfiguration.imports}.
 *
 * <p>Later tickets add the controllers they need here.
 */
@SpringBootApplication
public class StarterTestApplication {

    @RestController
    static class PingController {

        @GetMapping("/ping")
        String ping() {
            return "pong";
        }
    }

    /** Routes for the registration test: derived keys, a fixed key, a shared route, a public one. */
    @RestController
    @RequestMapping("/api/v1/faturas")
    static class FaturaController {

        @GetMapping(produces = "application/json")
        String listJson() {
            return "[]";
        }

        // Same method and path as listJson, other output: one route, one key.
        @GetMapping(produces = "application/pdf")
        byte[] listPdf() {
            return new byte[0];
        }

        @GetMapping("/{id}")
        String read(@PathVariable String id) {
            return id;
        }

        @Permission(value = "starter-test.faturas.aprovar.update",
                name = "Aprovar fatura", description = "Aprova uma fatura pendente")
        @PostMapping("/{id}/aprovar")
        String approve(@PathVariable String id) {
            return id;
        }

        @Public
        @GetMapping("/publicas")
        String publicList() {
            return "[]";
        }
    }
}
