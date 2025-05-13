package org.cdpg.dx.common.exception;

public class NoRowFoundException extends DxPgException {
    public NoRowFoundException(String message) {
        super(DxErrorCodes.PG_NO_ROW_ERROR, message);
    }

    public NoRowFoundException(String message, Throwable cause) {
        super(DxErrorCodes.PG_NO_ROW_ERROR, message, cause);
    }
}