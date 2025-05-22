package org.cdpg.dx.common.exception;

public class DxAuthException extends BaseDxException {

    public DxAuthException(String message) {
        super(DxErrorCodes.AUTH_ERROR, message);
    }

    public DxAuthException(String message, Throwable cause) {
        super(DxErrorCodes.AUTH_ERROR, message, cause);
    }
}



