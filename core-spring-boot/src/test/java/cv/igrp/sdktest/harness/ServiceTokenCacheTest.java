package cv.igrp.sdktest.harness;

import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.api.M2MApi;
import cv.igrp.platform.access.client.model.ResourceDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * v3-18 (#26): the service authenticates to AM with its own client_credentials token,
 * kept in memory until shortly before it expires, and never with the static sync-token.
 */
class ServiceTokenCacheTest extends AbstractStarterTest {

    @Autowired
    ApiClient accessClient;

    @Test
    @DisplayName("Two calls to AM within the token's lifetime request no new token, and both carry it")
    void oneTokenForTwoCalls() throws Exception {
        M2MApi m2m = new M2MApi(accessClient);
        ResourceDTO resource = new ResourceDTO();
        resource.setName(SERVICE_NAME);
        m2m.syncResources(resource, SERVICE_NAME);
        long tokensBefore = AM.count("POST", "/oauth2/token");

        m2m.syncResources(resource, SERVICE_NAME);
        m2m.syncResources(resource, SERVICE_NAME);

        assertEquals(tokensBefore, AM.count("POST", "/oauth2/token"));
        StubAccessManagement.Request last = AM.lastRequest("POST", "/api/m2m/sync/resources");
        assertEquals("Bearer " + StubAccessManagement.ACCESS_TOKEN, last.header("Authorization"));
        assertNull(last.header("X-Machine-Auth-Token"));
    }
}
