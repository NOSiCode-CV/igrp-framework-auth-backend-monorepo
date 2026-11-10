package cv.igrp.framework.auth.core.audit.revision;

import cv.igrp.framework.auth.core.audit.CorrelationIds;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Fills {@link IgrpRevision}'s username and trace id from the thread that makes
 * the change. Envers creates the listener itself, so it holds no Spring beans.
 */
public class IgrpRevisionListener implements RevisionListener {

    /** Who a change is attributed to when no user is authenticated (schedulers, startup). */
    public static final String SYSTEM = "system";

    @Override
    public void newRevision(Object revisionEntity) {
        IgrpRevision revision = (IgrpRevision) revisionEntity;
        revision.setUsername(currentUsername());
        // Outside a request (schedulers, startup) the change still gets an id of its own.
        revision.setTraceId(CorrelationIds.current().orElseGet(() -> CorrelationIds.resolve(null)));
    }

    /** {@code preferred_username}, else the subject or principal name, else {@link #SYSTEM}. */
    static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM;
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String preferred = jwt.getClaimAsString("preferred_username");
            return preferred != null ? preferred : jwt.getSubject();
        }
        return authentication.getName();
    }
}
