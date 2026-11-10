package cv.igrp.framework.auth.core.audit.revision;

import cv.igrp.framework.auth.core.audit.CorrelationIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

/** v3-24 (#31): who a revision is attributed to, the same way AM's auditor resolves it. */
class IgrpRevisionListenerTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void aJwtUserIsTheirPreferredUsername() {
        authenticate(jwt().claim("preferred_username", "alice").build());

        assertThat(newRevision().getUsername()).isEqualTo("alice");
    }

    @Test
    void aJwtWithoutPreferredUsernameIsItsSubject() {
        authenticate(jwt().build());

        assertThat(newRevision().getUsername()).isEqualTo("subject-1");
    }

    @Test
    void aNonJwtPrincipalIsItsName() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("m2m-client", null, AuthorityUtils.NO_AUTHORITIES));

        assertThat(newRevision().getUsername()).isEqualTo("m2m-client");
    }

    @Test
    void anAnonymousOrMissingUserIsSystem() {
        assertThat(newRevision().getUsername()).isEqualTo(IgrpRevisionListener.SYSTEM);

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(newRevision().getUsername()).isEqualTo(IgrpRevisionListener.SYSTEM);
    }

    @Test
    void theTraceIdIsTheRequestCorrelationId() {
        MDC.put(CorrelationIds.MDC_KEY, "order-9");

        assertThat(newRevision().getTraceId()).isEqualTo("order-9");
    }

    private static IgrpRevision newRevision() {
        IgrpRevision revision = new IgrpRevision();
        new IgrpRevisionListener().newRevision(revision);
        return revision;
    }

    private static Jwt.Builder jwt() {
        return Jwt.withTokenValue("token").header("alg", "none").subject("subject-1");
    }

    private static void authenticate(Jwt jwt) {
        // As the resource server builds it: with authorities, so it is authenticated.
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES));
    }
}
