package cv.igrp.framework.auth.core.audit;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * v3-21 (#29): creates {@code audit_event} and {@code audit_outbox} at startup, with the
 * starter's own Flyway history ({@code igrp_audit_schema_history}), and makes them
 * append-only for the application's database role.
 *
 * <p>PostgreSQL only. On another database the service still starts, a warning says why,
 * and {@link Audit#record} fails instead of losing events.
 */
public class AuditSchema implements InitializingBean {

    static final String HISTORY_TABLE = "igrp_audit_schema_history";
    static final String LOCATION = "classpath:db/igrp-audit";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditSchema.class);

    private final DataSource dataSource;
    private final String appRole;
    private String unavailableReason = "The iGRP business audit has not been set up yet";

    /**
     * @param appRole the database role the application uses; blank means the role it connects with
     */
    public AuditSchema(DataSource dataSource, String appRole) {
        this.dataSource = dataSource;
        this.appRole = appRole;
    }

    @Override
    public void afterPropertiesSet() {
        String role;
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (!"PostgreSQL".equals(product)) {
                unavailableReason = "The iGRP business audit supports PostgreSQL only; this service uses "
                        + product + ", so audit records cannot be written.";
                LOGGER.warn("[Audit] {}", unavailableReason);
                return;
            }
            role = appRole.isBlank() ? connection.getMetaData().getUserName() : appRole;
            warnIfSuperuser(connection, role);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not inspect the database for the iGRP business audit", e);
        }

        Flyway.configure()
                .dataSource(dataSource)
                .locations(LOCATION)
                .table(HISTORY_TABLE)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                // The role goes into GRANT statements inside double quotes: double any quote it holds.
                .placeholders(Map.of("appRole", role.replace("\"", "\"\"")))
                .load()
                .migrate();
        warnIfOwner(role);
        unavailableReason = null;
    }

    /** {@code null} when the tables are ready, otherwise why records cannot be written. */
    String unavailableReason() {
        return unavailableReason;
    }

    // The owner of a table can grant itself back what was revoked: append-only then holds only
    // while nobody does that. Say so, so production can split the migration and application roles.
    private void warnIfOwner(String role) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT tableowner FROM pg_tables WHERE tablename = 'audit_event' AND schemaname = current_schema()")) {
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next() && role.equals(rs.getString(1))) {
                    LOGGER.warn("[Audit] The application role '{}' owns audit_event, so it could grant itself UPDATE "
                            + "and DELETE again. For a strict append-only audit, run migrations with another role "
                            + "and set igrp.audit.app-role to the application's role.", role);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not check the owner of audit_event", e);
        }
    }

    private static void warnIfSuperuser(Connection connection, String role) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT rolsuper FROM pg_roles WHERE rolname = ?")) {
            statement.setString(1, role);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next() && rs.getBoolean(1)) {
                    LOGGER.warn("[Audit] The application role '{}' is a superuser: the database cannot keep "
                            + "audit_event append-only for it. Connect the application with an ordinary role.", role);
                }
            }
        }
    }
}
