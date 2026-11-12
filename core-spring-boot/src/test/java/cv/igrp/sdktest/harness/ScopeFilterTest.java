package cv.igrp.sdktest.harness;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v3-14 (#22): every query on a scoped entity shows only the rows the route key's scope allows,
 * with the strategies combined by OR, without the controller doing anything.
 *
 * <pre>
 *  id  owner        role    department
 *  1   OWNER        GESTOR  DEPT_A
 *  2   someone      CONTAB  DEPT_B
 *  3   someone      GESTOR  DEPT_C
 *  4   someone      OUTRO   DEPT_A
 * </pre>
 */
@ExtendWith(OutputCaptureExtension.class)
class ScopeFilterTest extends AbstractStarterTest {

    private static final String LIST = "starter-test.scoped.faturas.list";
    private static final String READ = "starter-test.scoped.faturas.read";
    private static final String OWNER = "owner-" + UUID.randomUUID();

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    ScopedInvoiceRepository invoices;

    @Autowired
    JdbcTemplate jdbc;

    /** Plain SQL, so seeding loads no entity (and does not use up the once-per-type warning). */
    @BeforeEach
    void rows() {
        jdbc.update("delete from scoped_invoice");
        jdbc.batchUpdate("insert into scoped_invoice (id, owner_id, role_code, department_code) values (?, ?, ?, ?)",
                List.of(new Object[]{1L, OWNER, "GESTOR", "DEPT_A"}, new Object[]{2L, "someone", "CONTAB", "DEPT_B"},
                        new Object[]{3L, "someone", "GESTOR", "DEPT_C"}, new Object[]{4L, "someone", "OUTRO", "DEPT_A"}));
    }

    /** Lists the rows as a user whose LIST and READ keys have this scope, roles and departments. */
    private ResultActions as(String sub, String scope, List<String> roles, List<String> departments, String path)
            throws Exception {
        writePermissionSet(redis, sub, Map.of(LIST, scope, READ, scope), roles, departments);
        return mvc.perform(get(path).with(jwt().jwt(token -> token.subject(sub))));
    }

    private static String user() {
        return "user-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("all: every row, no filter")
    void all() throws Exception {
        as(user(), "all", List.of(), List.of(), "/scoped/faturas").andExpect(content().json("[1,2,3,4]"));
    }

    @Test
    @DisplayName("own: the rows whose owner is the caller")
    void own() throws Exception {
        as(OWNER, "own", List.of(), List.of(), "/scoped/faturas").andExpect(content().json("[1]"));
    }

    @Test
    @DisplayName("by_role: the rows assigned to one of the caller's roles")
    void byRole() throws Exception {
        as(user(), "by_role", List.of("CONTAB", "OUTRO"), List.of(), "/scoped/faturas").andExpect(content().json("[2,4]"));
    }

    @Test
    @DisplayName("department: the rows in the caller's visible department subtree")
    void department() throws Exception {
        as(user(), "department", List.of(), List.of("DEPT_A"), "/scoped/faturas").andExpect(content().json("[1,4]"));
    }

    @Test
    @DisplayName("Combined scopes are a union: by_role,own sees both sets of rows")
    void combined() throws Exception {
        as(OWNER, "by_role,own", List.of("CONTAB"), List.of(), "/scoped/faturas").andExpect(content().json("[1,2]"));
    }

    @Test
    @DisplayName("by_role for a user with no roles: valid SQL, no rows")
    void noRoles() throws Exception {
        as(user(), "by_role", List.of(), List.of(), "/scoped/faturas")
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("A read by id outside the scope finds nothing (404); inside, it finds the row")
    void readById() throws Exception {
        String sub = user();
        as(sub, "department", List.of(), List.of("DEPT_B"), "/scoped/faturas/3").andExpect(status().isNotFound());
        as(sub, "department", List.of(), List.of("DEPT_B"), "/scoped/faturas/2").andExpect(content().string("2"));
    }

    @Test
    @DisplayName("An unknown scope code refuses the request instead of guessing")
    void unknownCode() throws Exception {
        as(user(), "everything", List.of(), List.of(), "/scoped/faturas").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A key with no scope in Redis refuses the request")
    void missingScope() throws Exception {
        String sub = user();
        redis.opsForSet().add("permissions:user:" + sub + ":app:" + APPLICATION_CODE, "~", LIST);

        mvc.perform(get("/scoped/faturas").with(jwt().jwt(token -> token.subject(sub)))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("On a Redis miss the scope, roles and departments come from AM")
    void fromAm() throws Exception {
        AM.stub("GET", "/api/users/me/permissions", 200, "{\"keys\":[\"" + LIST + "\"],\"scopes\":{\"" + LIST
                + "\":\"department\"},\"roles\":[\"GESTOR\"],\"departments\":[\"DEPT_B\"]}");

        mvc.perform(get("/scoped/faturas").with(jwt().jwt(token -> token.subject(user()))))
                .andExpect(content().json("[2]"));
    }

    @Test
    @DisplayName("Root sees every row")
    void root() throws Exception {
        mvc.perform(get("/scoped/faturas").with(jwt().jwt(token -> token.subject(user()).claim("is_super_admin", true))))
                .andExpect(content().json("[1,2,3,4]"));
    }

    @Test
    @DisplayName("Loaded with no scope, as in a background job: every row, and a warning")
    void noScope(CapturedOutput output) {
        assertEquals(4, invoices.findAll().size());
        assertTrue(output.getOut().contains("[Data Scope] " + ScopedInvoice.class.getName() + " loaded without a scope"),
                output.getOut());
    }
}
