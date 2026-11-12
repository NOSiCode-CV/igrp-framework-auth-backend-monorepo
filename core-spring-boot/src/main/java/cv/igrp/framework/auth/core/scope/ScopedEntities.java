package cv.igrp.framework.auth.core.scope;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Filter;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Which entity types carry the starter's scope filter, cached per type. {@code @Filter} is repeatable. */
final class ScopedEntities {

    private static final Map<Class<?>, Boolean> SCOPED = new ConcurrentHashMap<>();

    private ScopedEntities() {
    }

    /** @param entity a loaded entity or a proxy of one */
    static boolean isScoped(Object entity) {
        return SCOPED.computeIfAbsent(Hibernate.getClassLazy(entity), t -> AnnotatedElementUtils.findMergedRepeatableAnnotations(t, Filter.class)
                .stream().anyMatch(filter -> IgrpScope.FILTER.equals(filter.name())));
    }
}
