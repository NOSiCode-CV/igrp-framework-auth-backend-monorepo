package cv.igrp.framework.auth.core.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * v3-21 (#29): the business audit API ({@link Audit}) and its tables ({@link AuditSchema}),
 * for a service with a {@code DataSource}, a transaction manager and Flyway on its classpath.
 *
 * <p>{@code igrp.audit.app-role} names the database role the application uses, which gets
 * the append-only grants. Blank (the default) means the role the application connects with.
 */
@org.springframework.boot.autoconfigure.AutoConfiguration(afterName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
        "org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration"})
@ConditionalOnClass(name = {"org.flywaydb.core.Flyway", "org.springframework.jdbc.core.JdbcTemplate"})
@ConditionalOnSingleCandidate(DataSource.class)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AuditSchema igrpAuditSchema(DataSource dataSource, @Value("${igrp.audit.app-role:}") String appRole) {
        return new AuditSchema(dataSource, appRole);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(PlatformTransactionManager.class)
    Audit igrpAudit(DataSource dataSource, PlatformTransactionManager transactionManager, AuditSchema schema,
                    @Value("${igrp.security.service-name:${spring.application.name:}}") String service) {
        return new Audit(new JdbcTemplate(dataSource), new TransactionTemplate(transactionManager), schema,
                service, Clock.systemUTC());
    }
}
