package cv.igrp.framework.auth.core.model;

import java.util.List;
import java.util.Set;

/**
 * What AM asks the IdP for when it creates or updates a service client.
 *
 * @param clientId     the client id AM wants; {@code null} lets the IdP choose one.
 *                     IdPs with dynamic registration always choose it and ignore this.
 * @param clientName   display name of the client
 * @param grantTypes   OAuth grant types, e.g. {@code client_credentials}
 * @param redirectUris redirect URIs, empty for a client that only uses {@code client_credentials}
 */
public record ClientRegistration(String clientId, String clientName, Set<String> grantTypes, List<String> redirectUris) {

    public ClientRegistration {
        grantTypes = grantTypes == null ? Set.of() : Set.copyOf(grantTypes);
        redirectUris = redirectUris == null ? List.of() : List.copyOf(redirectUris);
    }
}
