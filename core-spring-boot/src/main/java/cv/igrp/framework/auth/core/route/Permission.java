package cv.igrp.framework.auth.core.route;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fixes the permission key of a route, independent of its path, so renaming the
 * path keeps the key and its assignments.
 *
 * <p>The key is the full key, service name included:
 * {@code @Permission("faturacao-api.faturas.aprovar.update")}. The service refuses
 * to start if the prefix is not its own service name, if the key is used on another
 * route, or if the action (last segment) is not in {@link PermissionKeys#ACTIONS}.
 *
 * <p>Not needed for protection: every route has a key, annotated or not.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Permission {

    /** The full key, {@code {service}.{resource path}.{action}}. */
    String value();

    /** Display name shown to administrators when the key is first created. */
    String name() default "";

    /** Description shown to administrators when the key is first created. */
    String description() default "";
}
