package cv.igrp.framework.auth.core.field;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** v3-16: the guard in the less common Jackson shapes, and who counts as a caller. */
class FieldPermissionModuleTest {

    private static final String KEY = "svc.notas.margem.read";

    private final JsonMapper json = JsonMapper.builder().addModule(new FieldPermissionModule()).build();

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    record AsArray(int id, @FieldPermission(KEY) int margin) {
    }

    record Inner(int id, @FieldPermission(KEY) int margin) {
    }

    record Outer(@JsonUnwrapped(prefix = "nota_") Inner nota) {
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void signIn(Authentication caller) {
        SecurityContextHolder.getContext().setAuthentication(caller);
    }

    private static Authentication withKeys(String... keys) {
        return UsernamePasswordAuthenticationToken.authenticated("maria", null, AuthorityUtils.createAuthorityList(keys));
    }

    @Test
    @DisplayName("An object written as an array keeps the slot of a hidden field, as null")
    void arrayShape() {
        signIn(withKeys(KEY));
        assertEquals("[1,7]", json.writeValueAsString(new AsArray(1, 7)));

        signIn(withKeys());
        assertEquals("[1,null]", json.writeValueAsString(new AsArray(1, 7)));
    }

    @Test
    @DisplayName("An unwrapped, renamed property stays guarded")
    void unwrapped() {
        signIn(withKeys(KEY));
        assertEquals("{\"nota_id\":1,\"nota_margin\":7}", json.writeValueAsString(new Outer(new Inner(1, 7))));

        signIn(withKeys());
        assertEquals("{\"nota_id\":1}", json.writeValueAsString(new Outer(new Inner(1, 7))));
    }

    @Test
    @DisplayName("Anonymous and unauthenticated callers never see a guarded field, whatever they hold")
    void notACaller() {
        signIn(new AnonymousAuthenticationToken("k", "anonymous", AuthorityUtils.createAuthorityList(KEY)));
        assertEquals("{\"id\":1}", json.writeValueAsString(new Inner(1, 7)));

        signIn(UsernamePasswordAuthenticationToken.unauthenticated("maria", "x"));
        assertEquals("{\"id\":1}", json.writeValueAsString(new Inner(1, 7)));
    }
}
