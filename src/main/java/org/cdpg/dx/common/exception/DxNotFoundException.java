package org.cdpg.dx.common.exception;

public class DxNotFoundException extends BaseDxException {

    public DxNotFoundException(String message) {
        super(DxErrorCodes.NOT_FOUND, message);
    }

    public DxNotFoundException(String message, Throwable cause) {
        super(DxErrorCodes.NOT_FOUND, message, cause);
    }
}



