package cv.igrp.framework.auth.core.scope;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.TransactionDefinition;

import java.sql.SQLException;

/**
 * Turns the scope filter on at the start of every transaction, in the session that runs its
 * queries (v3-14). Spring Data repository methods run in a transaction too, so they are
 * covered. With no scope (no request, or a route with no scope) and with {@code all}, the
 * filter stays off.
 */
public class ScopingJpaDialect extends HibernateJpaDialect {

    @Override
    public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition) throws SQLException {
        Object transaction = super.beginTransaction(entityManager, definition);
        apply(entityManager.unwrap(Session.class), IgrpScope.current());
        return transaction;
    }

    static void apply(Session session, IgrpScope.Context scope) {
        if (scope == null || scope.all()) {
            session.disableFilter(IgrpScope.FILTER);
            return;
        }
        session.enableFilter(IgrpScope.FILTER)
                .setParameter("igrpOwn", scope.own())
                .setParameter("igrpSub", scope.sub() == null ? IgrpScope.NONE : scope.sub())
                .setParameter("igrpByRole", scope.byRole())
                .setParameterList("igrpRoles", scope.roleParameter())
                .setParameter("igrpDepartment", scope.department())
                .setParameterList("igrpDepartments", scope.departmentParameter());
    }
}
