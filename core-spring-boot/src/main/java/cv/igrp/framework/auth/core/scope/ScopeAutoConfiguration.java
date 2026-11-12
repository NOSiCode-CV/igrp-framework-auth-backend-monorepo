package cv.igrp.framework.auth.core.scope;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Data scopes (v3-14) for services with JPA on Hibernate: every transaction turns the scope
 * filter on ({@link ScopingJpaDialect}), reads by id are checked against it
 * ({@link ScopedLoadByKey}), and scoped entities loaded with no scope are logged
 * ({@link UnscopedLoadWarning}). The filter itself is declared in this package's package-info,
 * which is added to the persistence unit's managed packages.
 */
@org.springframework.boot.autoconfigure.AutoConfiguration
@ConditionalOnClass({EntityManagerFactory.class, SessionFactoryImplementor.class, JpaTransactionManager.class})
public class ScopeAutoConfiguration {

    @Bean
    static BeanPostProcessor igrpScopeJpaHooks() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                if (bean instanceof PersistenceManagedTypes types) {
                    return withScopeFilter(types);
                }
                if (bean instanceof JpaTransactionManager transactions) {
                    transactions.setJpaDialect(new ScopingJpaDialect());
                } else if (bean instanceof EntityManagerFactory factory) {
                    registerListeners(factory);
                }
                return bean;
            }
        };
    }

    /** Adds this package, whose package-info declares the scope filter, to the persistence unit. */
    static PersistenceManagedTypes withScopeFilter(PersistenceManagedTypes types) {
        String scopePackage = IgrpScope.class.getPackageName();
        if (types.getManagedPackages().contains(scopePackage)) {
            return types;
        }
        List<String> packages = new ArrayList<>(types.getManagedPackages());
        packages.add(scopePackage);
        return PersistenceManagedTypes.of(types.getManagedClassNames(), packages);
    }

    /** Once per session factory, however many times the factory is post-processed. */
    static void registerListeners(EntityManagerFactory factory) {
        EventListenerRegistry registry = factory.unwrap(SessionFactoryImplementor.class).getServiceRegistry()
                .requireService(EventListenerRegistry.class);
        for (Object listener : registry.getEventListenerGroup(EventType.POST_LOAD).listeners()) {
            if (listener instanceof UnscopedLoadWarning) {
                return;
            }
        }
        registry.appendListeners(EventType.POST_LOAD, new UnscopedLoadWarning());
        registry.appendListeners(EventType.LOAD, new ScopedLoadByKey());
    }
}
