package cv.igrp.sdktest.harness;

import cv.igrp.framework.auth.core.audit.CorrelationIds;
import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.ApiException;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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

    /** v3-24: writes an audited entity, so a request produces an Envers revision. */
    @RestController
    static class NoteController {

        private final NoteRepository notes;

        NoteController(NoteRepository notes) {
            this.notes = notes;
        }

        @PostMapping("/notes")
        Long create(@RequestBody String text) {
            return notes.save(new Note(text)).getId();
        }
    }

    /** v3-24: shows the correlation id bound to the request, and makes a call to AM. */
    @RestController
    static class CorrelationController {

        private final ApiClient am;

        CorrelationController(ApiClient am) {
            this.am = am;
        }

        @GetMapping("/correlation")
        String correlation() {
            return MDC.get(CorrelationIds.MDC_KEY);
        }

        @GetMapping("/correlation/call-am")
        String callAm() throws ApiException, java.io.IOException, InterruptedException {
            return am.invokeAPIRaw("/api/echo", "GET", Map.of(), null, Map.of());
        }
    }
}
