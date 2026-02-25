package cv.igrp.framework.auth.core.security;

import java.util.Set;

public interface IAuthorizationServiceAdapter {

    Set<String> getRoles(String jwt);
    Set<String> getPermissions(String jwt);
    Set<String> getDepartments(String jwt);

}
