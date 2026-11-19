package cv.igrp.framework.auth.core.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashSet;

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
    @DisplayName("Packaged audit migrations have unique Flyway versions")
    void migrationVersionsAreUnique() throws Exception {
        var resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:db/igrp-audit/V*__*.sql");
        assertTrue(resources.length > 0, "Audit migrations must be packaged");
        var versions = new HashSet<MigrationVersion>();
        for (var resource : resources) {
            String filename = resource.getFilename();
            assertNotNull(filename);
            var version = MigrationVersion.fromVersion(filename.substring(1, filename.indexOf("__")));
            assertTrue(versions.add(version), "Duplicate audit migration version: " + filename);
        }
    }

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
