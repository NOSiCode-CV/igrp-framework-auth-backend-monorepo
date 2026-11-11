package cv.igrp.sdktest.harness;

import cv.igrp.framework.auth.core.security.PermissionAuthenticationConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v3-08 (#16): on a controller's request handlers the route decides and a leftover
 * {@code @PreAuthorize} is ignored; on code with no route {@code @PreAuthorize} checks
 * the caller's permission keys, read from the Redis set AM writes (or AM on a miss).
 */
class MethodSecurityTest extends AbstractStarterTest {

    private static final String APPROVE = "starter-test.faturas.aprovar.update";

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    PermissionAuthenticationConverter converter;

    @Autowired
    StarterTestApplication.InvoiceApprovals approvals;

    @Autowired
    ApplicationContext context;

    @AfterEach
    void clearCaller() {
        SecurityContextHolder.clearContext();
    }

    private String cachedUser(String... keys) {
        String sub = "user-" + UUID.randomUUID();
        redis.opsForSet().add("permissions:user:" + sub + ":app:" + APPLICATION_CODE,
                Stream.concat(Stream.of("~"), Stream.of(keys)).toArray(String[]::new));
        return sub;
    }

    /** The authentication the resource server builds for this caller, made the current one. */
    private AbstractAuthenticationToken signIn(String sub) {
        Jwt token = Jwt.withTokenValue("token-of-" + sub).header("alg", "none").subject(sub)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        AbstractAuthenticationToken authentication = converter.convert(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    @Test
    @DisplayName("Controller: a @PreAuthorize that denies everyone is ignored when the route key is granted")
    void controllerGranted() throws Exception {
        String sub = cachedUser("starter-test.legacy.guarded.list");

        mvc.perform(get("/legacy/guarded").with(jwt().jwt(token -> token.subject(sub))))
                .andExpect(status().isOk()).andExpect(content().string("reached"));
    }

    @Test
    @DisplayName("Controller: without the route key the route filter denies, @PreAuthorize or not")
    void controllerDenied() throws Exception {
        String sub = cachedUser("starter-test.ping.list");

        mvc.perform(get("/legacy/guarded").with(jwt().jwt(token -> token.subject(sub))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Service: @PreAuthorize(hasAuthority) passes when the Redis set has the key")
    void serviceGranted() {
        signIn(cachedUser(APPROVE));

        assertEquals("approved 7", approvals.approve("7"));
    }

    @Test
    @DisplayName("Service: @PreAuthorize(hasAuthority) denies when the Redis set lacks the key")
    void serviceDenied() {
        signIn(cachedUser("starter-test.faturas.read"));

        assertThrows(AccessDeniedException.class, () -> approvals.approve("7"));
    }

    @Test
    @DisplayName("Service: on a Redis miss the keys come from AM, with the caller's token")
    void serviceFromAm() {
        AM.stub("GET", "/api/users/me/permissions", 200, permissionSet(List.of(APPROVE)));
        String sub = "user-" + UUID.randomUUID();
        signIn(sub);

        assertEquals("approved 8", approvals.approve("8"));
        assertEquals("Bearer token-of-" + sub, AM.lastRequest("GET", "/api/users/me/permissions").header("Authorization"));
    }

    @Test
    @DisplayName("The permission keys are loaded only when something asks for them, then once")
    void authoritiesAreLazy() {
        AM.stub("GET", "/api/users/me/permissions", 200, permissionSet(List.of(APPROVE)));
        long before = AM.count("GET", "/api/users/me/permissions");

        AbstractAuthenticationToken authentication = signIn("user-" + UUID.randomUUID());
        assertEquals(before, AM.count("GET", "/api/users/me/permissions"));

        authentication.getAuthorities();
        authentication.getAuthorities();
        assertEquals(before + 1, AM.count("GET", "/api/users/me/permissions"));
    }

    @Test
    @DisplayName("The 0.3 starter has no igrpAuthorization bean")
    void noLegacyBean() {
        assertFalse(context.containsBean("igrpAuthorization"));
    }
}
