package cv.igrp.sdktest.startup.reuse;

import cv.igrp.framework.auth.core.route.Permission;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** One @Permission key on two routes. */
@SpringBootApplication
public class ReuseApp {

    @RestController
    static class Routes {

        @Permission("failing-test.faturas.list")
        @GetMapping("/faturas")
        String list() {
            return "[]";
        }

        @Permission("failing-test.faturas.list")
        @GetMapping("/notas")
        String otherList() {
            return "[]";
        }
    }
}
