package cv.igrp.framework.auth.core.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * v3-21 (#29): the business audit API ({@link Audit}) and its tables ({@link AuditSchema}),
 * for a service with a {@code DataSource} and Flyway (core and its PostgreSQL module) on its
 * classpath.
 *
 * <p>{@code igrp.audit.app-role} names the database role the application uses, which gets
 * the append-only grants; blank (the default) means the role the application connects with.
 * {@code igrp.audit.enabled=false} turns the audit off: no tables, no {@link Audit} bean.
 *
 * <p>The audit tables are created after the service's own database initialisation (its
 * Flyway or Liquibase), so a fresh database never looks "non-empty without history" to it.
 */
@org.springframework.boot.autoconfigure.AutoConfiguration(afterName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
        "org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration"})
@ConditionalOnClass(name = {"org.flywaydb.core.Flyway", "org.flywaydb.database.postgresql.PostgreSQLDatabaseType",
        "org.springframework.jdbc.core.JdbcTemplate"})
@ConditionalOnProperty(name = "igrp.audit.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnSingleCandidate(DataSource.class)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @DependsOnDatabaseInitialization
    public AuditSchema igrpAuditSchema(DataSource dataSource, @Value("${igrp.audit.app-role:}") String appRole) {
        return new AuditSchema(dataSource, appRole);
    }

    /**
     * Records join the caller's transaction and refuse to run without one (MANDATORY): an event
     * exists only together with the action it describes.
     */
    @Bean
    @ConditionalOnMissingBean
    public Audit igrpAudit(DataSource dataSource, PlatformTransactionManager transactionManager, AuditSchema schema,
                           @Value("${igrp.security.service-name:${spring.application.name:}}") String service) {
        TransactionTemplate callersTransaction = new TransactionTemplate(transactionManager);
        callersTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
        return new Audit(new JdbcTemplate(dataSource), callersTransaction, schema, service, Clock.systemUTC());
    }
}
