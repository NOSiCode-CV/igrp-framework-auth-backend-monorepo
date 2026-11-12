package cv.igrp.sdktest.harness;

import cv.igrp.framework.auth.core.audit.Audit;
import cv.igrp.framework.auth.core.audit.CorrelationIds;
import cv.igrp.framework.auth.core.field.FieldPermission;
import cv.igrp.framework.auth.core.route.Permission;
import cv.igrp.framework.auth.core.route.Public;
import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.ApiException;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
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

    /** v3-08: a leftover @PreAuthorize that would deny everyone; the route decision applies instead. */
    @RestController
    static class LegacyGuardedController {

        @PreAuthorize("denyAll()")
        @GetMapping("/legacy/guarded")
        String guarded() {
            return "reached";
        }
    }

    /** v3-08: code with no route stays protected by @PreAuthorize, against the caller's permission keys. */
    @Service
    public static class InvoiceApprovals {

        @PreAuthorize("hasAuthority('starter-test.faturas.aprovar.update')")
        public String approve(String id) {
            return "approved " + id;
        }
    }

    /** v3-16: a response with guarded fields, one of them in a nested object. */
    public record InvoiceView(long id, BigDecimal total,
                              @FieldPermission(value = "starter-test.faturas.margem.read", name = "Margem da fatura",
                                      description = "Ver a margem de lucro de uma fatura")
                              BigDecimal margin,
                              CustomerView customer) {
    }

    public record CustomerView(String name, @FieldPermission("starter-test.clientes.nif.read") String nif) {
    }

    @RestController
    static class InvoiceViewController {

        private static InvoiceView invoice(long id) {
            return new InvoiceView(id, new BigDecimal("100.00"), new BigDecimal("12.50"),
                    new CustomerView("Maria", "123456789"));
        }

        @GetMapping("/views/faturas")
        List<InvoiceView> list() {
            return List.of(invoice(1), invoice(2));
        }

        @GetMapping("/views/faturas/{id}")
        InvoiceView read(@PathVariable long id) {
            return invoice(id);
        }
    }

    /** v3-14: lists and reads of a scoped entity; the starter filters them, the controller does nothing. */
    @RestController
    static class ScopedInvoiceController {

        private final ScopedInvoiceRepository invoices;

        ScopedInvoiceController(ScopedInvoiceRepository invoices) {
            this.invoices = invoices;
        }

        @GetMapping("/scoped/faturas")
        List<Long> list() {
            return invoices.findAll().stream().map(ScopedInvoice::getId).sorted().toList();
        }

        @GetMapping("/scoped/faturas/{id}")
        Long read(@PathVariable long id) {
            return invoices.findById(id).map(ScopedInvoice::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
    }

    /** v3-07: public in this environment through igrp.security.public-paths, not through @Public. */
    @RestController
    static class InfraController {

        @GetMapping("/infra/status")
        String status() {
            return "up";
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

    /** v3-21: records a business action inside the action's transaction, then commits or rolls back. */
    @RestController
    @RequestMapping("/audit")
    static class AuditController {

        private final Audit audit;

        AuditController(Audit audit) {
            this.audit = audit;
        }

        @Transactional
        @PostMapping("/commit")
        void commit(@RequestParam String resource) {
            audit.record("fatura.aprovada", resource, Map.of("total", 100));
        }

        @Transactional
        @PostMapping("/rollback")
        void rollback(@RequestParam String resource) {
            audit.record("fatura.aprovada", resource, Map.of("total", 100));
            throw new ActionFailed();
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    static class ActionFailed extends RuntimeException {
    }
}
