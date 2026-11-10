package cv.igrp.framework.auth.core.audit.revision;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * v3-24 (#31): makes {@link IgrpRevision} the Envers revision of any service
 * that has Envers on its classpath.
 *
 * <p>{@code @AutoConfigurationPackage} adds the revision's package to the
 * default entity scan. Startup then fails, with the fix in the message, when
 * either of the two things a service must do is missing: the revision entity
 * in its entity scan (an explicit {@code @EntityScan} replaces the default), or
 * the {@code username} and {@code trace_id} columns in {@code revinfo}. Set
 * {@code igrp.audit.revision.enabled=false} to keep Hibernate's default revision,
 * for example in a service that has Envers on its classpath without using it,
 * or that defines its own {@code @RevisionEntity} (Envers allows only one).
 */
@org.springframework.boot.autoconfigure.AutoConfiguration
@ConditionalOnClass(name = {"org.hibernate.envers.RevisionEntity", "jakarta.persistence.EntityManagerFactory"})
@ConditionalOnProperty(name = "igrp.audit.revision.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigurationPackage(basePackageClasses = IgrpRevision.class)
public class RevisionAutoConfiguration {

    static final String COLUMNS_FIX = "ALTER TABLE revinfo ADD COLUMN username VARCHAR(255); "
            + "ALTER TABLE revinfo ADD COLUMN trace_id VARCHAR(64);";

    @Bean
    SmartInitializingSingleton igrpRevisionCheck(ObjectProvider<EntityManagerFactory> entityManagerFactory,
                                                 ObjectProvider<DataSource> dataSource) {
        return () -> entityManagerFactory.ifAvailable(emf -> {
            checkEntityScan(emf);
            dataSource.ifAvailable(ds -> checkColumns(emf, ds));
        });
    }

    private static void checkEntityScan(EntityManagerFactory emf) {
        try {
            emf.getMetamodel().entity(IgrpRevision.class);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("The iGRP Envers revision " + IgrpRevision.class.getName()
                    + " is not a managed entity, so revisions would lose username and trace_id. "
                    + "Add its package to your @EntityScan: cv.igrp.framework.auth.core.audit.revision "
                    + "(or set igrp.audit.revision.enabled=false).", e);
        }
    }

    /**
     * Fails when {@code revinfo} lacks the two columns. Matters under
     * {@code ddl-auto=none}: with {@code validate} Hibernate reports the missing
     * column itself, and with {@code update} it adds them. Reads the catalog,
     * so it honours {@code hibernate.default_schema}.
     */
    private static void checkColumns(EntityManagerFactory emf, DataSource dataSource) {
        Object configured = emf.getProperties().get("hibernate.default_schema");
        try (Connection connection = dataSource.getConnection()) {
            String schema = configured instanceof String name && !name.isBlank() ? name : connection.getSchema();
            Set<String> columns = new HashSet<>();
            DatabaseMetaData metaData = connection.getMetaData();
            for (String table : new String[]{"revinfo", "REVINFO"}) {
                try (ResultSet rs = metaData.getColumns(connection.getCatalog(), schema, table, null)) {
                    while (rs.next()) {
                        columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                    }
                }
            }
            if (!columns.containsAll(Set.of("username", "trace_id"))) {
                throw new IllegalStateException("The revinfo table lacks the iGRP revision columns. "
                        + "Add them in a migration: " + COLUMNS_FIX
                        + " (or set igrp.audit.revision.enabled=false).");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not check the revinfo columns of the iGRP revision", e);
        }
    }
}
