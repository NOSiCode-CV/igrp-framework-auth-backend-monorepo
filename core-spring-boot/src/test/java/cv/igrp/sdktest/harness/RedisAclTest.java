package cv.igrp.sdktest.harness;

import cv.igrp.framework.auth.core.route.PermissionRedis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v3-09 (#17): only AM writes permission keys. The starter reads the shared Redis as the
 * read-only user the Redis ACL gives services, so a compromised service cannot grant itself
 * or anyone else a permission.
 */
class RedisAclTest extends AbstractStarterTest {

    /** The service's own connection: the default user, standing in for AM's writer. */
    @Autowired
    StringRedisTemplate serviceRedis;

    @Autowired
    PermissionRedis permissionRedis;

    private String userWith(String... keys) {
        String sub = "user-" + UUID.randomUUID();
        writePermissionSet(serviceRedis, sub, keys);
        return sub;
    }

    private static String set(String sub) {
        return "permissions:user:" + sub + ":app:" + APPLICATION_CODE;
    }

    /** Redis refused the command for the user's ACL (the cause Lettuce raises, under Spring's wrapper). */
    private static void assertNoPermission(Exception refused) {
        Throwable cause = refused;
        while (cause != null && (cause.getMessage() == null || !cause.getMessage().contains("NOPERM"))) {
            cause = cause.getCause();
        }
        assertNotNull(cause, () -> "Not an ACL refusal: " + refused);
    }

    @Test
    @DisplayName("The starter reads the set AM wrote, as the read-only user, and decides from it")
    void readsAsReader() throws Exception {
        String sub = userWith("starter-test.faturas.list");
        long fetches = AM.count("GET", "/api/users/me/permissions");

        assertEquals(Set.of("~", "starter-test.faturas.list"), permissionRedis.template().opsForSet().members(set(sub)));
        mvc.perform(get("/api/v1/faturas").with(jwt().jwt(jwt -> jwt.subject(sub)))).andExpect(status().isOk());
        assertEquals(fetches, AM.count("GET", "/api/users/me/permissions"));
    }

    @Test
    @DisplayName("A write to a permission key through the starter's connection is refused")
    void writeRefused() {
        String sub = userWith("starter-test.faturas.list");
        StringRedisTemplate reader = permissionRedis.template();

        Exception added = assertThrows(Exception.class,
                () -> reader.opsForSet().add(set(sub), "starter-test.faturas.aprovar.update"));
        assertNoPermission(added);
        assertThrows(Exception.class, () -> reader.opsForHash().put(set(sub) + ":scopes", "starter-test.faturas.list", "all"));
        assertThrows(Exception.class, () -> reader.delete(set(sub)));
        assertThrows(Exception.class, () -> reader.opsForSet().add("permissions:user:" + UUID.randomUUID() + ":app:x", "a"));

        assertEquals(Set.of("~", "starter-test.faturas.list"), serviceRedis.opsForSet().members(set(sub)));
    }

    @Test
    @DisplayName("Keys outside the permission namespace cannot even be read")
    void otherKeysRefused() {
        serviceRedis.opsForValue().set("service:own:data", "x");

        Exception read = assertThrows(Exception.class, () -> permissionRedis.template().opsForValue().get("service:own:data"));
        assertNoPermission(read);
    }
}
