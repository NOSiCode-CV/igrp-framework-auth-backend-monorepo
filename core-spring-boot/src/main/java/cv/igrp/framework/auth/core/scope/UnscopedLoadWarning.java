package cv.igrp.framework.auth.core.scope;

import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logs a warning when a scoped entity is loaded with no scope at all: a background job, or a
 * route the caller reached with no scope. It does not fail, so jobs keep working, and it warns
 * once per entity type to keep the log readable.
 */
public class UnscopedLoadWarning implements PostLoadEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnscopedLoadWarning.class);

    private final Set<Class<?>> warned = ConcurrentHashMap.newKeySet();

    @Override
    public void onPostLoad(PostLoadEvent event) {
        Object entity = event.getEntity();
        Class<?> type = entity.getClass();
        if (IgrpScope.current() == null && ScopedEntities.isScoped(entity) && warned.add(type)) {
            LOGGER.warn("[Data Scope] {} loaded without a scope (no request scope: a background job?); "
                    + "its rows are not filtered", type.getName());
        }
    }
}
