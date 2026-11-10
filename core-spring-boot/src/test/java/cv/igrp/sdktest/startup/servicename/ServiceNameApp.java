package cv.igrp.sdktest.startup.servicename;

import cv.igrp.framework.auth.core.route.Permission;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** A @Permission key under igrp.security.service-name, not spring.application.name. */
@SpringBootApplication
public class ServiceNameApp {

    @RestController
    static class Routes {

        @Permission("billing.faturas.list")
        @GetMapping("/faturas")
        String list() {
            return "[]";
        }
    }
}
