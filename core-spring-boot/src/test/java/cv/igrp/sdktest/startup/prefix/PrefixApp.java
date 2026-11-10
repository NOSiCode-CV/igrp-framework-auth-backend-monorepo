package cv.igrp.sdktest.startup.prefix;

import cv.igrp.framework.auth.core.route.Permission;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** A @Permission key in another service's namespace. */
@SpringBootApplication
public class PrefixApp {

    @RestController
    static class Routes {

        @Permission("other-service.faturas.list")
        @GetMapping("/faturas")
        String list() {
            return "[]";
        }
    }
}
