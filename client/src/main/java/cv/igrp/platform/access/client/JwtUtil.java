package cv.igrp.platform.access.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JwtUtil {

    /**
     * Whether the given JWT is root's: {@code is_super_admin} is {@code true} (AM 0.3 thin
     * tokens, v3-25), or, in an AM 0.2 token, the role "DEPT_IGRP.superadmin" is under
     * resource_access -> access-management -> roles.
     *
     * @param jwt the raw JWT string (Authorization header value without "Bearer ")
     * @return true for root, false otherwise
     */
    public static boolean hasSuperAdminRole(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return false;
        }

        try {
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8
            );

            JsonMapper mapper = JsonMapper.builder().configureForJackson2().build();
            JsonNode root = mapper.readTree(payloadJson);

            if (root.path("is_super_admin").asBoolean(false)) {
                return true;
            }

            JsonNode rolesNode = root
                    .path("resource_access")
                    .path("access-management")
                    .path("roles");

            if (!rolesNode.isArray()) {
                return false;
            }

            for (JsonNode role : rolesNode) {
                if ("DEPT_IGRP.superadmin".equals(role.asString())) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

}
