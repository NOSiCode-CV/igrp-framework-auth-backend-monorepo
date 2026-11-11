package cv.igrp.framework.auth.core.field;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Hides a response field from callers whose permission set lacks the key (v3-16). It is
 * applied when the response is serialised, so every endpoint returning the type is covered,
 * nested objects and collections included. Root sees every field.
 *
 * <pre>{@code
 * public record FaturaView(Long id, BigDecimal total,
 *         @FieldPermission(value = "faturacao-api.faturas.margem.read", name = "Margem da fatura")
 *         BigDecimal margem) {}
 * }</pre>
 *
 * The key follows the route keys' form, {@code {service}.{resource}.{action}}; a key outside
 * the service's namespace or with an unknown action fails the startup. The starter registers
 * the keys with AM next to the routes, so administrators can grant them.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface FieldPermission {

    /** The permission key the caller needs to see the field. */
    String value();

    /** Display name for administrators; the key when blank. */
    String name() default "";

    /** What seeing this field allows. */
    String description() default "";
}
