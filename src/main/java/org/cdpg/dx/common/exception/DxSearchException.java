package org.cdpg.dx.common.exception;

public class DxSearchException extends BaseDxException {

    public DxSearchException(String message) {
        super(DxErrorCodes.SEARCH_ERROR, message);
    }

    public DxSearchException(String message, Throwable cause) {
        super(DxErrorCodes.SEARCH_ERROR, message, cause);

    }

    public DxSearchException(int code, String message) {
        super(code, message);
    }

    public DxSearchException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}



