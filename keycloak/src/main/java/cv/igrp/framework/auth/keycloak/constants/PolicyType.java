package cv.igrp.framework.auth.keycloak.constants;

public enum PolicyType {

    ROLE("role")
    ;

    private final String name;

    PolicyType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
