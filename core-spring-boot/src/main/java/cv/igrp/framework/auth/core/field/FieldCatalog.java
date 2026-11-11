package cv.igrp.framework.auth.core.field;

import com.fasterxml.jackson.annotation.JsonInclude;
import cv.igrp.framework.auth.core.route.PermissionKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Finds the {@link FieldPermission} keys of a service in its own packages, at startup
 * (v3-16), so they can be registered with AM next to the routes. A key that breaks the
 * key rules fails the startup, as a {@code @Permission} key does.
 */
public final class FieldCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldCatalog.class);

    /** One field key, with the display name and description of its first declaration. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Field(String key, String name, String description) {
    }

    private FieldCatalog() {
    }

    /**
     * @param packages the application's base packages
     * @throws IllegalStateException when a key is outside the service's namespace or has an unknown action
     */
    public static List<Field> collect(String service, List<String> packages, ClassLoader classLoader) {
        Map<String, Field> fields = new TreeMap<>();
        List<String> errors = new ArrayList<>();
        for (String basePackage : packages) {
            for (BeanDefinition candidate : scanner().findCandidateComponents(basePackage)) {
                Class<?> type = load(candidate.getBeanClassName(), classLoader);
                if (type != null) {
                    annotated(type).forEach(member -> add(service, member, fields, errors));
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid @FieldPermission keys in service '" + service + "':\n - "
                    + String.join("\n - ", errors));
        }
        return List.copyOf(fields.values());
    }

    /** Every class of the packages, records and nested classes included, not only Spring components. */
    private static ClassPathScanningCandidateComponentProvider scanner() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                return true;
            }
        };
        scanner.addIncludeFilter((reader, factory) -> true);
        return scanner;
    }

    private static Class<?> load(String className, ClassLoader classLoader) {
        try {
            return ClassUtils.forName(className, classLoader);
        } catch (ClassNotFoundException | LinkageError ex) {
            LOGGER.debug("[Field Permissions] Skipping {}: {}", className, ex.getMessage());
            return null;
        }
    }

    private static Stream<AnnotatedElement> annotated(Class<?> type) {
        return Stream.concat(Stream.of(type.getDeclaredFields()), Stream.of(type.getDeclaredMethods()))
                .filter(member -> member.isAnnotationPresent(FieldPermission.class))
                .map(AnnotatedElement.class::cast);
    }

    private static void add(String service, AnnotatedElement element, Map<String, Field> fields, List<String> errors) {
        FieldPermission permission = element.getAnnotation(FieldPermission.class);
        Member member = (Member) element;
        String label = member.getDeclaringClass().getName() + "." + member.getName();
        PermissionKeys.problems(service, permission.value()).forEach(problem ->
                errors.add(label + ": @FieldPermission(\"" + permission.value() + "\") " + problem));
        fields.putIfAbsent(permission.value(), new Field(permission.value(),
                blankToNull(permission.name()), blankToNull(permission.description())));
    }

    private static String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }
}
