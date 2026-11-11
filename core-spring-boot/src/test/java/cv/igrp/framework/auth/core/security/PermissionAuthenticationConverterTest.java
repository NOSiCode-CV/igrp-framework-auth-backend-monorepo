package cv.igrp.framework.auth.core.security;

import cv.igrp.framework.auth.core.route.PermissionLookup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** v3-08: the bearer token's authorities are the bearer factor plus the permission keys, loaded on first use. */
class PermissionAuthenticationConverterTest {

    private final PermissionLookup lookup = mock(PermissionLookup.class);
    private final PermissionAuthenticationConverter converter = new PermissionAuthenticationConverter(lookup);

    private static Jwt jwt(String sub) {
        return Jwt.withTokenValue("token-" + sub).header("alg", "none").subject(sub)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }

    private static List<String> names(AbstractAuthenticationToken token) {
        return token.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    @DisplayName("Authenticated, named by sub; the keys are read once, on the first look at the authorities")
    void lazyKeys() {
        when(lookup.permissions("user-1", "token-user-1")).thenReturn(Set.of("svc.a.list"));

        AbstractAuthenticationToken token = converter.convert(jwt("user-1"));
        assertTrue(token.isAuthenticated());
        assertEquals("user-1", token.getName());
        verifyNoInteractions(lookup);

        assertEquals(List.of("FACTOR_BEARER", "svc.a.list"), names(token));
        names(token);
        verify(lookup, times(1)).permissions("user-1", "token-user-1");
    }

    @Test
    @DisplayName("Equal as JWT authentications are; a serialised copy keeps only the bearer factor")
    void equalityAndSerialisation() throws Exception {
        when(lookup.permissions("user-1", "token-user-1")).thenReturn(Set.of("svc.a.list"));
        AbstractAuthenticationToken token = converter.convert(jwt("user-1"));

        assertEquals(token, token);
        assertEquals(token.hashCode(), token.hashCode());
        assertNotEquals(token, converter.convert(jwt("user-2")));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(token);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            AbstractAuthenticationToken copy = (AbstractAuthenticationToken) in.readObject();
            assertEquals(List.of("FACTOR_BEARER"), names(copy));
        }
    }
}
