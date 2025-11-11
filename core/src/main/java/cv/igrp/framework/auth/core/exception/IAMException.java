package cv.igrp.framework.auth.core.exception;

public class IAMException extends Exception {

    public IAMException(String message) {
        super(message);
    }

    public IAMException(String message, Throwable cause) {
        super(message, cause);
    }

    public IAMException(Throwable cause) {
        super(cause);
    }
}
