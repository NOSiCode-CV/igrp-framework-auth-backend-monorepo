package cv.igrp.framework.auth.core.scope;

import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;

/**
 * Reads by id ({@code find}, {@code findById}) ignore Hibernate filters, so a guessed id could
 * show a row outside the caller's scope (note N8). After such a read, this checks the row with
 * a query, which the scope filter does apply to; outside the scope the read finds nothing.
 */
public class ScopedLoadByKey implements LoadEventListener {

    @Override
    public void onLoad(LoadEvent event, LoadType loadType) {
        Object entity = event.getResult();
        if (entity == null || (loadType != GET && loadType != LOAD)) {
            return;
        }
        EventSource session = event.getSession();
        if (session.getEnabledFilter(IgrpScope.FILTER) == null || !ScopedEntities.isScoped(entity)) {
            return;
        }
        Long visible = session.createSelectionQuery(
                        "select count(e) from " + event.getEntityClassName() + " e where id(e) = :id", Long.class)
                .setParameter("id", event.getEntityId())
                .getSingleResult();
        if (visible == 0) {
            session.evict(entity);
            event.setResult(null);
        }
    }
}
