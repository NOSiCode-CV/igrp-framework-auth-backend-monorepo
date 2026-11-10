package cv.igrp.sdktest.harness;

import cv.igrp.framework.auth.core.audit.Audit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3-21 (#29): the harness's default database is H2. The audit supports PostgreSQL only,
 * so the service still starts, and a record fails loudly instead of being lost.
 */
class AuditWithoutPostgresTest extends AbstractStarterTest {

    @Autowired
    Audit audit;

    @Test
    @DisplayName("On a database other than PostgreSQL, recording fails with the reason instead of dropping the event")
    void recordFailsOutsidePostgres() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> audit.record("fatura.aprovada", "faturas/1", Map.of()));
        assertTrue(failure.getMessage().contains("PostgreSQL"), failure.getMessage());
    }
}
