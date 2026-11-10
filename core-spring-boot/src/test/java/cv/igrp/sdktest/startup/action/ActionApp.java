package cv.igrp.sdktest.startup.action;

import cv.igrp.framework.auth.core.route.Permission;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** A @Permission action outside the allowed set. */
@SpringBootApplication
public class ActionApp {

    @RestController
    static class Routes {

        @Permission("failing-test.faturas.approve")
        @PostMapping("/faturas/aprovar")
        String approve() {
            return "ok";
        }
    }
}
