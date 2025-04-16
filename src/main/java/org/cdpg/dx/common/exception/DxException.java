package org.cdpg.dx.common.exception;

public class DxException extends RuntimeException {
    private final String errorCode;

    public DxException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}