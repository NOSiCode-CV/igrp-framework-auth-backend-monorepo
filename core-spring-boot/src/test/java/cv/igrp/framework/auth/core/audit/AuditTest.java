package cv.igrp.framework.auth.core.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v3-21 (#29): what {@link Audit#record} fills in by itself, case by case, without a database.
 * The writes themselves are tested against PostgreSQL in {@code AuditHarnessTest}.
 */
class AuditTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private final AuditSchema schema = mock(AuditSchema.class);
    private final Audit audit = new Audit(jdbc, transactions, schema, "faturacao-api",
            Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void runTransactionsInline() {
        doAnswer(call -> {
            call.getArgument(0, Consumer.class).accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
    }

    @AfterEach
    void clearUser() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("A JWT user: sub as user, act.sub as agent, the service, SUCCESS and the clock's time")
    void jwtUserWithAgent() {
        authenticate(jwt(Map.of("sub", "user-1", "act", Map.of("sub", "agent-x"))));

        Object[] row = recordAndCaptureEvent(Map.of("total", 100));

        assertEquals("fatura.aprovada", row[1]);
        assertEquals("faturas/42", row[2]);
        assertEquals("{\"total\":100}", row[3]);
        assertEquals("user-1", row[4]);
        assertEquals("faturacao-api", row[5]);
        assertEquals("agent-x", row[6]);
        assertEquals(Audit.SUCCESS, row[7]);
        assertEquals(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC), row[9]);
    }

    @Test
    @DisplayName("No agent when the token has no act claim, or an act claim without sub")
    void noAgent() {
        authenticate(jwt(Map.of("sub", "user-1")));
        assertNull(recordAndCaptureEvent(null)[6]);

        authenticate(jwt(Map.of("sub", "user-1", "act", Map.of("client_id", "agent-x"))));
        assertNull(recordAndCaptureEvent(null)[6]);

        authenticate(jwt(Map.of("sub", "user-1", "act", "not-an-object")));
        assertNull(recordAndCaptureEvent(null)[6]);
    }

    @Test
    @DisplayName("The system is the user without authentication, for an anonymous one and for an unauthenticated one")
    void systemUser() {
        assertEquals(Audit.SYSTEM_USER, recordAndCaptureEvent(null)[4]);

        authenticate(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertEquals(Audit.SYSTEM_USER, recordAndCaptureEvent(null)[4]);

        authenticate(UsernamePasswordAuthenticationToken.unauthenticated("maria", "secret"));
        assertEquals(Audit.SYSTEM_USER, recordAndCaptureEvent(null)[4]);
    }

    @Test
    @DisplayName("A non-JWT login is recorded by its name; a JWT without sub by the token's name")
    void otherLogins() {
        authenticate(UsernamePasswordAuthenticationToken.authenticated("maria", null, AuthorityUtils.NO_AUTHORITIES));
        assertEquals("maria", recordAndCaptureEvent(null)[4]);

        authenticate(new JwtAuthenticationToken(jwt(Map.of("scope", "x")), AuthorityUtils.NO_AUTHORITIES, "svc-client"));
        assertEquals("svc-client", recordAndCaptureEvent(null)[4]);
    }

    @Test
    @DisplayName("A null payload is stored as SQL NULL; the outbox row still carries the whole event")
    void nullPayload() {
        Object[] row = recordAndCaptureEvent(null);
        assertNull(row[3]);

        ArgumentCaptor<Object[]> outbox = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(startsWith("INSERT INTO audit_outbox"), outbox.capture());
        String event = (String) outbox.getValue()[2];
        assertTrue(event.contains("\"resource\":\"faturas/42\"") && event.contains("\"service\":\"faturacao-api\""), event);
    }

    @Test
    @DisplayName("Blank event or resource is refused before anything is written")
    void blankArguments() {
        assertThrows(IllegalArgumentException.class, () -> audit.record(" ", "faturas/42", null));
        assertThrows(IllegalArgumentException.class, () -> audit.record("fatura.aprovada", null, null));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("Without the audit tables the reason is thrown, and nothing is written")
    void tablesUnavailable() {
        when(schema.unavailableReason()).thenReturn("supports PostgreSQL only");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> audit.record("fatura.aprovada", "faturas/42", null));
        assertEquals("supports PostgreSQL only", failure.getMessage());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("Without a transaction the error says the record must run inside the action's transaction")
    void noTransaction() {
        doThrow(new IllegalTransactionStateException("No existing transaction"))
                .when(transactions).executeWithoutResult(any());

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> audit.record("fatura.aprovada", "faturas/42", null));
        assertTrue(failure.getMessage().contains("inside the action's transaction"), failure.getMessage());
    }

    private Object[] recordAndCaptureEvent(Object payload) {
        org.mockito.Mockito.clearInvocations(jdbc);
        audit.record("fatura.aprovada", "faturas/42", payload);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(startsWith("INSERT INTO audit_event"), args.capture());
        return args.getValue();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none");
        claims.forEach(builder::claim);
        return builder.build();
    }

    // As the resource server builds it: with authorities, hence authenticated.
    private static void authenticate(Jwt jwt) {
        authenticate(new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES));
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
