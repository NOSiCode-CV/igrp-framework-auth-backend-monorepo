package cv.igrp.framework.auth.core.model;

public interface UserIdentity {

    String getId();
    String getUsername();
    String getFirstName();
    String getLastName();
    String getEmail();
    boolean isEnabled();

    //  Novos métodos
    String getExternalId();
    boolean isEmailVerified();

}
