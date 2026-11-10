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
            checkRoleName(role);
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
                .placeholders(Map.of("appRole", role))
                .load()
                .migrate();
        unavailableReason = null;
    }

    /** {@code null} when the tables are ready, otherwise why records cannot be written. */
    String unavailableReason() {
        return unavailableReason;
    }

    // The role goes into GRANT statements as a quoted identifier: accept plain names only.
    private static void checkRoleName(String role) {
        boolean valid = !role.isEmpty() && role.length() <= 63;
        for (int i = 0; valid && i < role.length(); i++) {
            char c = role.charAt(i);
            valid = Character.isLetterOrDigit(c) || c == '_';
        }
        if (!valid) {
            throw new IllegalStateException("igrp.audit.app-role must be a plain PostgreSQL role name "
                    + "(letters, digits, _; at most 63), got '" + role + "'");
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
