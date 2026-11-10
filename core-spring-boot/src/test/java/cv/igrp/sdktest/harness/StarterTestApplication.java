package cv.igrp.sdktest.harness;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
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
}
