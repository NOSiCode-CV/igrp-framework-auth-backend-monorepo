package cv.igrp.platform.access.client;

public class ApiException extends Exception {
    private int code = 0;

    public ApiException() {}

    public ApiException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public ApiException(Throwable throwable) {
        super(throwable);
    }

    public int getCode() {
        return code;
    }
}