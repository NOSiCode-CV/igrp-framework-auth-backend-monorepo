package cv.igrp.platform.access.client.constants;

public enum MenuEntryType {
    SYSTEM_PAGE("SYSTEM_PAGE", "System Page"),
        MENU_PAGE("MENU_PAGE", "Menu Page"),
        EXTERNAL_PAGE("EXTERNAL_PAGE", "External Page"),
        FOLDER("FOLDER", "Folder"),
        GROUP("GROUP", "Group")
    ;

    private final String code;
    private final String description;

    MenuEntryType(String code, String description) {
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
