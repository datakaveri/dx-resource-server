package org.cdpg.dx.common.exception;

public class DxValidationException extends BaseDxException {

    public DxValidationException(String message) {
        super(DxErrorCodes.VALIDATION_ERROR, message);
    }
}
