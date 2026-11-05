package cv.igrp.platform.access.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JwtUtil {

    /**
     * Checks whether the given JWT contains the role "DEPT_IGRP.superadmin"
     * under resource_access -> access-management -> roles.
     *
     * @param jwt the raw JWT string (Authorization header value without "Bearer ")
     * @return true if the role is present, false otherwise
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
