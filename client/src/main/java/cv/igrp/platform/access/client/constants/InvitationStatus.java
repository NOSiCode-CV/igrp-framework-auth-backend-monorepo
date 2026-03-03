package cv.igrp.platform.access.client.constants;

public enum InvitationStatus {

    PENDING("PENDING", "Pending"),
    ACCEPTED("ACCEPTED", "Accepted"),
    REJECTED("REJECTED", "Rejected"),
    CANCELED("CANCELED", "Canceled");

    private final String code;
    private final String description;

    InvitationStatus(String code, String description) {
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