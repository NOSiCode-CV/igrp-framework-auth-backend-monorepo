package cv.igrp.framework.auth.core.route;

import cv.igrp.framework.auth.core.audit.Audit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** v3-07: where the route filter's access decisions are recorded, case by case. */
@ExtendWith(OutputCaptureExtension.class)
class AccessAuditTest {

    private final Audit audit = mock(Audit.class);
    private final TransactionTemplate ownTransaction = mock(TransactionTemplate.class);

    @SuppressWarnings("unchecked")
    private void runInline() {
        doAnswer(call -> {
            call.getArgument(0, Consumer.class).accept(mock(TransactionStatus.class));
            return null;
        }).when(ownTransaction).executeWithoutResult(any());
    }

    @Test
    @DisplayName("With the audit tables: written through the audit, in its own transaction")
    void recorded() {
        when(audit.available()).thenReturn(true);
        runInline();

        new AccessAudit(audit, ownTransaction).denied("GET", "/faturas", "svc.faturas.list");

        verify(ownTransaction).executeWithoutResult(any());
        verify(audit).record("access.denied", "GET /faturas",
                Map.of("method", "GET", "path", "/faturas", "key", "svc.faturas.list"));
    }

    @Test
    @DisplayName("Without the audit, or with it unavailable: a WARN log line with the decision")
    void logged(CapturedOutput output) {
        new AccessAudit(null, null).root("POST", "/notas", null);
        when(audit.available()).thenReturn(false);
        new AccessAudit(audit, ownTransaction).denied("GET", "/x", "svc.x.list");

        assertTrue(output.getOut().contains("access.root POST /notas"), output.getOut());
        assertTrue(output.getOut().contains("access.denied GET /x"), output.getOut());
        verify(audit, never()).record(any(), any(), any());
    }

    @Test
    @DisplayName("A failed write is an ERROR log, not a silent loss and not a failed request")
    void failedWrite(CapturedOutput output) {
        when(audit.available()).thenReturn(true);
        doThrow(new IllegalStateException("db down")).when(ownTransaction).executeWithoutResult(any());

        new AccessAudit(audit, ownTransaction).denied("GET", "/faturas", "svc.faturas.list");

        assertTrue(output.getOut().contains("ERROR") && output.getOut().contains("Could not record access.denied"),
                output.getOut());
    }
}
