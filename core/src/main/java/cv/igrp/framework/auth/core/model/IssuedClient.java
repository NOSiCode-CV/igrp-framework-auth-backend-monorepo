package cv.igrp.framework.auth.core.model;

import java.time.Instant;

/**
 * A client's credentials as the IdP returns them on create or rotate. The secret is
 * meant to be shown to the administrator once and never stored by AM.
 *
 * @param clientId        the client id in the IdP
 * @param clientSecret    the new secret
 * @param secretExpiresAt when the secret expires, or {@code null} if it does not
 */
public record IssuedClient(String clientId, String clientSecret, Instant secretExpiresAt) {

    @Override
    public String toString() {
        return "IssuedClient[clientId=" + clientId + ", clientSecret=***, secretExpiresAt=" + secretExpiresAt + "]";
    }
}
