package org.cdpg.dx.common.exception;

public class DxBadRequestException extends BaseDxException {

    public DxBadRequestException(String message) {
        super(DxErrorCodes.BAD_REQUEST_ERROR, message);
    }

    public DxBadRequestException(String message, Throwable cause) {
        super(DxErrorCodes.BAD_REQUEST_ERROR, message, cause);
    }
}



