package org.cdpg.dx.common.exception;

public class DxSubscriptionException extends BaseDxException {

    public DxSubscriptionException(String message) {
        super(DxErrorCodes.SUBS_ERROR, message);
    }

    public DxSubscriptionException(String message, Throwable cause) {
        super(DxErrorCodes.SUBS_ERROR, message, cause);
    }

    public DxSubscriptionException(int code, String message) {
        super(code, message);
    }

    public DxSubscriptionException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}



