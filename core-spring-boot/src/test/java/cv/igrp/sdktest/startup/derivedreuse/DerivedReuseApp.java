package cv.igrp.sdktest.startup.derivedreuse;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Two paths that derive the same key: /faturas and /api/faturas. */
@SpringBootApplication
public class DerivedReuseApp {

    @RestController
    static class Routes {

        @GetMapping("/faturas")
        String list() {
            return "[]";
        }

        @GetMapping("/api/faturas")
        String otherList() {
            return "[]";
        }
    }
}
