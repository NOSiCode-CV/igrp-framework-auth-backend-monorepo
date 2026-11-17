package cv.igrp.platform.access.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** v3-25 (#33): root is read from {@code is_super_admin}; AM 0.2 tokens still work. */
class JwtUtilTest {

    private static String jwt(String payloadJson) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8)) + ".signature";
    }

    @Test
    @DisplayName("An AM 0.3 thin token with is_super_admin true is root")
    void thinTokenRoot() {
        assertTrue(JwtUtil.hasSuperAdminRole(jwt("{\"sub\":\"u-1\",\"is_super_admin\":true}")));
    }

    @Test
    @DisplayName("An AM 0.3 thin token with is_super_admin false, or without it, is not root")
    void thinTokenNotRoot() {
        assertFalse(JwtUtil.hasSuperAdminRole(jwt("{\"sub\":\"u-1\",\"is_super_admin\":false}")));
        assertFalse(JwtUtil.hasSuperAdminRole(jwt("{\"sub\":\"service-account\"}")));
    }

    @Test
    @DisplayName("An AM 0.2 token with the superadmin role under resource_access is still root")
    void legacyTokenRoot() {
        assertTrue(JwtUtil.hasSuperAdminRole(jwt(
                "{\"resource_access\":{\"access-management\":{\"roles\":[\"DEPT_X.user\",\"DEPT_IGRP.superadmin\"]}}}")));
        assertFalse(JwtUtil.hasSuperAdminRole(jwt(
                "{\"resource_access\":{\"access-management\":{\"roles\":[\"DEPT_X.user\"]}}}")));
    }

    @Test
    @DisplayName("No token, or something that is not a JWT, is not root")
    void notAToken() {
        assertFalse(JwtUtil.hasSuperAdminRole(null));
        assertFalse(JwtUtil.hasSuperAdminRole(" "));
        assertFalse(JwtUtil.hasSuperAdminRole("not-a-jwt"));
        assertFalse(JwtUtil.hasSuperAdminRole("a.%%%.c"));
    }
}
