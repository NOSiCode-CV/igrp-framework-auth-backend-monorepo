package cv.igrp.framework.auth.core.route;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AgentCallMetricsFilterTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void countsRequestsEvenWhenForbiddenWithoutAnAuditEvent() throws Exception {
        var registry = new SimpleMeterRegistry();
        var jwt = Jwt.withTokenValue("unused").header("alg", "RS256").subject("user")
                .claim("act", Map.of("sub", "agent-a")).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        var filter = new AgentCallMetricsFilter(registry, "billing");
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) ->
                ((jakarta.servlet.http.HttpServletResponse) response).setStatus(403));
        assertEquals(1, registry.get("igrp.agent.calls").tag("agent", "agent-a").counter().count());
    }

    @Test void anonymousRequestsDoNotCreateAgentSeries() throws Exception {
        var registry = new SimpleMeterRegistry();
        new AgentCallMetricsFilter(registry, "billing").doFilter(new MockHttpServletRequest(),
                new MockHttpServletResponse(), (request, response) -> { });
        assertTrue(registry.getMeters().isEmpty());
    }
}
