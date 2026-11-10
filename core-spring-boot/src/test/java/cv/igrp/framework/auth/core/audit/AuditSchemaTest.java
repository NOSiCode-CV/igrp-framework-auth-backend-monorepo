package cv.igrp.framework.auth.core.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * v3-21 (#29): the startup decisions of {@link AuditSchema} that need no real database.
 * Migrations and grants are tested against PostgreSQL in {@code AuditHarnessTest}.
 */
class AuditSchemaTest {

    @Test
    @DisplayName("Before startup, records are refused with a reason")
    void notSetUpYet() {
        assertNotNull(new AuditSchema(mock(DataSource.class), "").unavailableReason());
    }

    @Test
    @DisplayName("A database that cannot be reached stops the startup with the cause")
    void unreachableDatabase() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        SQLException cause = new SQLException("connection refused");
        when(dataSource.getConnection()).thenThrow(cause);

        AuditSchema schema = new AuditSchema(dataSource, "");
        IllegalStateException failure = assertThrows(IllegalStateException.class, schema::afterPropertiesSet);
        assertEquals(cause, failure.getCause());
    }

    @Test
    @DisplayName("Another database leaves the service running and records refused, naming the database")
    void otherDatabase() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");

        AuditSchema schema = new AuditSchema(dataSource, "");
        schema.afterPropertiesSet();

        assertTrue(schema.unavailableReason().contains("MySQL"), schema.unavailableReason());
    }
}
