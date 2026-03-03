package cv.igrp.platform.access.client.constants;

public enum AppType {

    EXTERNAL("EXTERNAL", "External"),
    INTERNAL("INTERNAL", "Internal"),
    SYSTEM("SYSTEM", "System")
    ;

    private final String code;
    private final String description;

    AppType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

}
