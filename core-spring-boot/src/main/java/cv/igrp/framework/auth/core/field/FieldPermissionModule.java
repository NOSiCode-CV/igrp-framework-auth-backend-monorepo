package cv.igrp.framework.auth.core.field;

import cv.igrp.framework.auth.core.security.RootAccess;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.io.Serial;
import java.util.List;

/**
 * The Jackson 3 side of {@link FieldPermission} (v3-16): every bean property carrying the
 * annotation is written only when the current caller may see it. Spring Boot adds this
 * module to the application's {@code JsonMapper}, so it applies to every response.
 *
 * <p>The caller may see the field when it is root, or when its authorities (its permission
 * keys, v3-08) contain the key. With no caller, as outside a request, the field is hidden.
 */
public class FieldPermissionModule extends SimpleModule {

    @Serial
    private static final long serialVersionUID = 1L;

    public FieldPermissionModule() {
        super("igrp-field-permission");
        setSerializerModifier(new GuardFields());
    }

    /** Whether the current caller may see a field guarded by {@code key}. */
    static boolean visible(String key) {
        Authentication caller = SecurityContextHolder.getContext().getAuthentication();
        if (caller == null || !caller.isAuthenticated() || caller instanceof AnonymousAuthenticationToken) {
            return false;
        }
        if (caller instanceof JwtAuthenticationToken jwt && RootAccess.isRoot(jwt.getToken())) {
            return true;
        }
        for (GrantedAuthority authority : caller.getAuthorities()) {
            if (key.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static final class GuardFields extends ValueSerializerModifier {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier bean,
                                                         List<BeanPropertyWriter> properties) {
            return properties.stream().map(GuardFields::guard).toList();
        }

        private static BeanPropertyWriter guard(BeanPropertyWriter property) {
            FieldPermission permission = property.getAnnotation(FieldPermission.class);
            return permission == null ? property : new GuardedProperty(property, permission.value());
        }
    }

    /** A property written only when {@link #visible} says so; otherwise it is left out. */
    static final class GuardedProperty extends BeanPropertyWriter {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String key;

        GuardedProperty(BeanPropertyWriter base, String key) {
            super(base);
            this.key = key;
        }

        private GuardedProperty(GuardedProperty base, PropertyName newName) {
            super(base, newName);
            this.key = base.key;
        }

        /** A renamed copy (e.g. under {@code @JsonUnwrapped(prefix = ...)}) keeps the guard. */
        @Override
        protected BeanPropertyWriter _new(PropertyName newName) {
            return new GuardedProperty(this, newName);
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator generator, SerializationContext context) throws Exception {
            if (visible(key)) {
                super.serializeAsProperty(bean, generator, context);
            }
        }

        @Override
        public void serializeAsElement(Object bean, JsonGenerator generator, SerializationContext context) throws Exception {
            if (visible(key)) {
                super.serializeAsElement(bean, generator, context);
            } else {
                serializeAsOmittedElement(bean, generator, context);
            }
        }
    }
}
