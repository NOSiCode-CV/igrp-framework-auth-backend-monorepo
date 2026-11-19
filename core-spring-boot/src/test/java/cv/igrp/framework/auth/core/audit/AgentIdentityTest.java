package cv.igrp.framework.auth.core.audit;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AgentIdentityTest {
    private JwtAuthenticationToken authentication(Map<String, Object> claims) {
        var jwt = Jwt.withTokenValue("test-token").header("alg", "RS256").subject("service")
                .claims(values -> values.putAll(claims)).build();
        return new JwtAuthenticationToken(jwt, List.of());
    }

    @Test void absentOrInvalidActorsReturnEmptyMaps() {
        assertTrue(AgentIdentity.act(null).isEmpty());
        assertTrue(AgentIdentity.act(authentication(Map.of())).isEmpty());
        var invalid = authentication(Map.of("act", Map.of("sub", "agent", "act", Map.of("sub", ""))));
        assertTrue(AgentIdentity.act(invalid).isEmpty());
        assertNull(AgentIdentity.agent(invalid));
    }

    @Test void serviceAccountFallbackStillWorksWithoutAnActor() {
        var authentication = authentication(Map.of("principal_type", "SERVICE_ACCOUNT", "client_id", "client"));
        assertEquals("client", AgentIdentity.agent(authentication));
        assertTrue(AgentIdentity.act(authentication).isEmpty());
    }

    @Test void validChainIsPreservedAndOverlongChainIsRejected() {
        Map<String, Object> chain = Map.of("sub", "previous");
        chain = Map.of("sub", "current", "act", chain);
        assertEquals(chain, AgentIdentity.act(authentication(Map.of("act", chain))));
        for (int i = 0; i < 19; i++) chain = Map.of("sub", "agent", "act", chain);
        assertTrue(AgentIdentity.act(authentication(Map.of("act", chain))).isEmpty());
    }
}
