package cv.igrp.platform.access.client;

import cv.igrp.platform.access.client.errors.ProblemDetail;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Boot 4 migration gate — pins the exact JSON this SDK puts on the wire
 * and accepts back, for every DTO in {@code model}, and the error bodies it
 * reads into {@link ProblemDetail}.
 *
 * <p>Each DTO is filled with deterministic values derived from its field names,
 * serialized through {@link JsonUtil} (the mapper {@link ApiClient} uses for every
 * request body and response) and compared byte-for-byte, key order included,
 * with a golden file under {@code src/test/resources/wire-baseline}. The golden
 * file is then read back and re-serialized, which pins the read side too.
 *
 * <p>The golden files were recorded with the Jackson 2 client (0.2.0-beta.15).
 * When the client moves to Jackson 3 they must still match unchanged, which is
 * the proof that the access-management API sees the same bytes from old and new
 * SDK versions. Only {@link JsonUtil}'s static API is used, so this test needs
 * no change for that move.
 *
 * <p>Re-record (only for a deliberate, reviewed contract change):
 * {@code mvn test -Dtest=WireFormatBaselineTest -Dwire.baseline.record=true}
 */
class WireFormatBaselineTest {

    private static final Path BASELINE_DIR = Paths.get("src", "test", "resources", "wire-baseline");
    private static final boolean RECORD = Boolean.getBoolean("wire.baseline.record");
    private static final String[] PACKAGES = {
            "cv.igrp.platform.access.client.model",
            "cv.igrp.platform.access.client.model.reports"
    };
    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-02T03:04:05.678Z");

    @TestFactory
    Stream<DynamicTest> everyDtoMatchesItsBaseline() throws Exception {
        List<Class<?>> dtos = new ArrayList<>();
        for (String pkg : PACKAGES) {
            dtos.addAll(concreteClassesIn(pkg));
        }
        assertTrue(dtos.size() > 40, "expected the full DTO set, found " + dtos.size());
        return dtos.stream().map(type -> DynamicTest.dynamicTest(type.getSimpleName(), () -> verify(type)));
    }

    /**
     * {@link ProblemDetail} is read-only on the wire: the SDK never sends one,
     * it parses the RFC 7807 bodies the API returns. It is pinned on the read
     * side with the exact shape {@code GlobalExceptionHandler} produces, plus an
     * unknown extension member that must be tolerated.
     */
    @Test
    void malformedJsonStaysACheckedIOException() {
        // ApiClient's catch (IOException) blocks turn this into an ApiException;
        // Jackson 3 exceptions are unchecked and would skip them.
        assertThrows(IOException.class, () -> JsonUtil.deserialize("{\"title\":", ProblemDetail.class));
    }

    @Test
    void problemDetailFromTheApiIsReadAsBefore() throws Exception {
        String body = "{\"type\":\"about:blank\",\"title\":\"Role not found with code: ADMIN\",\"status\":404,"
                + "\"detail\":\"Role not found\",\"instance\":\"/api/roles/by-code/ADMIN\","
                + "\"code\":\"IGRP_AUTH_ROLE_NOT_FOUND_BY_CODE\",\"details\":{\"roleCode\":\"ADMIN\"},"
                + "\"timestamp\":\"2026-01-02T03:04:05Z\"}";

        ProblemDetail problem = JsonUtil.deserialize(body, ProblemDetail.class);

        assertEquals(URI.create("about:blank"), problem.getType());
        assertEquals("Role not found with code: ADMIN", problem.getTitle());
        assertEquals(404, problem.getStatus());
        assertEquals("Role not found", problem.getDetail());
        assertEquals(URI.create("/api/roles/by-code/ADMIN"), problem.getInstance());
        assertEquals("IGRP_AUTH_ROLE_NOT_FOUND_BY_CODE", problem.getCode());
        assertEquals(Map.of("roleCode", "ADMIN"), problem.getDetails());
        assertEquals("2026-01-02T03:04:05Z", problem.getProperties().get("timestamp"));
    }

    private void verify(Class<?> type) throws Exception {
        Object sample = fill(type, 0);
        String json = JsonUtil.serialize(sample);
        Path golden = BASELINE_DIR.resolve(type.getName() + ".json");

        if (RECORD) {
            Files.createDirectories(BASELINE_DIR);
            Files.writeString(golden, json, StandardCharsets.UTF_8);
            return;
        }

        assertTrue(Files.exists(golden), "no baseline for " + type.getName()
                + "; a new DTO needs one recorded with -Dwire.baseline.record=true");
        String expected = Files.readString(golden, StandardCharsets.UTF_8);
        assertEquals(expected, json, "request/response JSON changed for " + type.getName());

        Object back = JsonUtil.deserialize(expected, type);
        assertEquals(expected, JsonUtil.serialize(back), "read side changed for " + type.getName());
    }

    // ---- deterministic sample values ----

    private Object fill(Class<?> type, int depth) throws Exception {
        Object instance = newInstance(type);
        if (instance == null) {
            return null;
        }
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || field.isSynthetic()) {
                    continue;
                }
                field.setAccessible(true);
                field.set(instance, valueFor(field.getName(), field.getGenericType(), depth));
            }
        }
        return instance;
    }

    private Object valueFor(String name, Type type, int depth) throws Exception {
        if (type instanceof ParameterizedType parameterized) {
            Class<?> raw = (Class<?>) parameterized.getRawType();
            Type[] args = parameterized.getActualTypeArguments();
            if (Map.class.isAssignableFrom(raw)) {
                Map<Object, Object> map = new LinkedHashMap<>();
                map.put(name + "Key", valueFor(name + "Value", args[1], depth + 1));
                return map;
            }
            if (Collection.class.isAssignableFrom(raw)) {
                Collection<Object> collection = Set.class.isAssignableFrom(raw) ? new LinkedHashSet<>() : new ArrayList<>();
                Object element = valueFor(name + "Item", args[0], depth + 1);
                if (element != null) {
                    collection.add(element);
                }
                return collection;
            }
            return null;
        }
        if (!(type instanceof Class<?> cls)) {
            // Unresolved type variable (e.g. PageResponse<T>): a plain string.
            return name;
        }
        if (cls == String.class || cls == Object.class) return name;
        if (cls == Integer.class || cls == int.class) return 7;
        if (cls == Long.class || cls == long.class) return 42L;
        if (cls == Short.class || cls == short.class) return (short) 3;
        if (cls == Double.class || cls == double.class) return 1.5d;
        if (cls == Float.class || cls == float.class) return 2.5f;
        if (cls == Boolean.class || cls == boolean.class) return Boolean.TRUE;
        if (cls == BigDecimal.class) return new BigDecimal("12.34");
        if (cls == UUID.class) return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        if (cls == Instant.class) return FIXED_INSTANT;
        if (cls == LocalDateTime.class) return LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
        if (cls == LocalDate.class) return LocalDate.of(2026, 1, 2);
        if (cls == OffsetDateTime.class) return FIXED_INSTANT.atOffset(ZoneOffset.UTC);
        if (cls == byte[].class) return name.getBytes(StandardCharsets.UTF_8);
        if (cls.isEnum()) return cls.getEnumConstants()[0];
        if (Map.class.isAssignableFrom(cls)) return new LinkedHashMap<>(Map.of(name + "Key", name + "Value"));
        if (List.class.isAssignableFrom(cls)) return new ArrayList<>(List.of(name + "Item"));
        if (cls == URI.class) return URI.create("https://example.cv/" + name);
        if (cls.getName().startsWith("java.")) {
            throw new IllegalStateException("no sample value for JDK type " + cls.getName() + " (field " + name + ")");
        }
        if (depth >= 3 || cls.isInterface() || Modifier.isAbstract(cls.getModifiers())) return null;
        return fill(cls, depth + 1);
    }

    private static Object newInstance(Class<?> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    // ---- class discovery from the compiled output ----

    private static List<Class<?>> concreteClassesIn(String pkg) throws IOException, URISyntaxException, ClassNotFoundException {
        Path root = Paths.get(JsonUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path dir = root.resolve(pkg.replace('.', '/'));
        List<Class<?>> classes = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.sorted().toList()) {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith(".class") || fileName.contains("$")) {
                    continue;
                }
                Class<?> cls = Class.forName(pkg + "." + fileName.substring(0, fileName.length() - ".class".length()));
                if (cls.isEnum() || cls.isInterface() || Modifier.isAbstract(cls.getModifiers())) {
                    continue;
                }
                classes.add(cls);
            }
        }
        return classes;
    }
}
