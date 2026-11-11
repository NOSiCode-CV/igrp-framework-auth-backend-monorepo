package cv.igrp.sdktest.startup.fieldkey;

import cv.igrp.framework.auth.core.field.FieldPermission;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** A @FieldPermission key in another service's namespace. */
@SpringBootApplication
public class FieldKeyApp {

    public record FaturaView(long id, @FieldPermission("other-service.faturas.margem.read") long margem) {
    }

    @RestController
    static class Routes {

        @GetMapping("/faturas")
        FaturaView list() {
            return new FaturaView(1, 2);
        }
    }
}
