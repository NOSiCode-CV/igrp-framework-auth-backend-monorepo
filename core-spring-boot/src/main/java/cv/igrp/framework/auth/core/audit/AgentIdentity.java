package cv.igrp.framework.auth.core.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.LinkedHashMap;
import java.util.Map;

/** Agent identity comes exclusively from the authenticated JWT, never request headers. */
public final class AgentIdentity {
    private AgentIdentity() { }

    public static String agent(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) return null;
        Map<String, Object> act = act(authentication);
        if (act != null) return (String) act.get("sub");
        if ("SERVICE_ACCOUNT".equals(token.getToken().getClaimAsString("principal_type"))) {
            String client = token.getToken().getClaimAsString("client_id");
            return client == null || client.isBlank() ? token.getToken().getSubject() : client;
        }
        return null;
    }

    public static Map<String, Object> act(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) return null;
        return copy(token.getToken().getClaims().get("act"), 0);
    }

    private static Map<String, Object> copy(Object value, int depth) {
        if (!(value instanceof Map<?, ?> actor) || !(actor.get("sub") instanceof String sub)
                || sub.isBlank() || sub.length() > 255 || depth >= 20) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sub", sub);
        if (actor.get("iss") instanceof String issuer && issuer.length() <= 512) result.put("iss", issuer);
        if (actor.get("act") != null) {
            Map<String, Object> previous = copy(actor.get("act"), depth + 1);
            if (previous == null) return null;
            result.put("act", previous);
        }
        return result;
    }
}
