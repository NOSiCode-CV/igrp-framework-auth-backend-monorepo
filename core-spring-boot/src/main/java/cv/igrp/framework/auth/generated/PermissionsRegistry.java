package cv.igrp.framework.auth.generated;

public class PermissionsRegistry {
    public enum Permission {

        ;

        private final String code;
        private final String description;
        private final boolean enabled;

        Permission(String code, String description, boolean enabled) {
            this.code = code;
            this.description = description;
            this.enabled = enabled;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
        public boolean enabled() { return enabled; }

    }
}
