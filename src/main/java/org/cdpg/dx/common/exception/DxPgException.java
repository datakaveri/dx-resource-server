package org.cdpg.dx.common.exception;

public class DxPgException extends BaseDxException {

    public DxPgException(String message) {
        super(DxErrorCodes.PG_ERROR, message);
    }

    public DxPgException(String message, Throwable cause) {
        super(DxErrorCodes.PG_ERROR, message, cause);
    }

    public DxPgException(int code, String message) {
        super(code, message);
    }

    public DxPgException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}



