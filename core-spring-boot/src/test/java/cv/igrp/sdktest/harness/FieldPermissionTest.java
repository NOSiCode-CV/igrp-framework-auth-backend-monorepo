package cv.igrp.sdktest.harness;

import cv.igrp.framework.auth.core.security.PermissionAuthenticationConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v3-16 (#23): a {@code @FieldPermission} field is in the response only when the caller's
 * permission set has its key, in nested objects and collections too; root sees every field.
 * The caller is authenticated by the starter's own converter, so its keys come from Redis.
 */
class FieldPermissionTest extends AbstractStarterTest {

    private static final String READ = "starter-test.views.faturas.read";
    private static final String LIST = "starter-test.views.faturas.list";
    private static final String MARGIN = "starter-test.faturas.margem.read";
    private static final String NIF = "starter-test.clientes.nif.read";

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    PermissionAuthenticationConverter converter;

    @Autowired
    JsonMapper jsonMapper;

    @AfterEach
    void clearCaller() {
        SecurityContextHolder.clearContext();
    }

    private RequestPostProcessor caller(boolean root, String... keys) {
        String sub = "user-" + UUID.randomUUID();
        redis.opsForSet().add("permissions:user:" + sub + ":app:" + APPLICATION_CODE,
                Stream.concat(Stream.of("~"), Stream.of(keys)).toArray(String[]::new));
        Jwt.Builder token = Jwt.withTokenValue("token-of-" + sub).header("alg", "none").subject(sub)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (root) {
            token.claim("is_super_admin", true);
        }
        return authentication(converter.convert(token.build()));
    }

    @Test
    @DisplayName("With the key the field is there; without it the field is left out, not null")
    void guardedField() throws Exception {
        mvc.perform(get("/views/faturas/3").with(caller(false, READ, MARGIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.margin").value(12.5))
                .andExpect(jsonPath("$.total").value(100.0));

        mvc.perform(get("/views/faturas/3").with(caller(false, READ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.margin").doesNotExist())
                .andExpect(jsonPath("$.total").value(100.0));
    }

    @Test
    @DisplayName("A guarded field in a nested object follows its own key")
    void nestedObject() throws Exception {
        mvc.perform(get("/views/faturas/3").with(caller(false, READ, NIF)))
                .andExpect(jsonPath("$.customer.nif").value("123456789"))
                .andExpect(jsonPath("$.margin").doesNotExist());

        mvc.perform(get("/views/faturas/3").with(caller(false, READ, MARGIN)))
                .andExpect(jsonPath("$.customer.name").value("Maria"))
                .andExpect(jsonPath("$.customer.nif").doesNotExist());
    }

    @Test
    @DisplayName("Every element of a collection is filtered")
    void collection() throws Exception {
        mvc.perform(get("/views/faturas").with(caller(false, LIST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].margin").doesNotExist())
                .andExpect(jsonPath("$[1].margin").doesNotExist())
                .andExpect(jsonPath("$[1].customer.nif").doesNotExist());

        mvc.perform(get("/views/faturas").with(caller(false, LIST, MARGIN, NIF)))
                .andExpect(jsonPath("$[0].margin").value(12.5))
                .andExpect(jsonPath("$[1].customer.nif").value("123456789"));
    }

    @Test
    @DisplayName("Root sees every field without holding any key")
    void root() throws Exception {
        mvc.perform(get("/views/faturas").with(caller(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].margin").value(12.5))
                .andExpect(jsonPath("$[0].customer.nif").value("123456789"));
    }

    @Test
    @DisplayName("With no caller, as outside a request, the application's JsonMapper hides the guarded fields")
    void noCaller() {
        String json = jsonMapper.writeValueAsString(new StarterTestApplication.InvoiceView(1, BigDecimal.ONE,
                BigDecimal.TEN, new StarterTestApplication.CustomerView("Maria", "123")));

        assertEquals("{\"id\":1,\"total\":1,\"customer\":{\"name\":\"Maria\"}}", json);
    }
}
