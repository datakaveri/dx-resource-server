package org.cdpg.dx.common.exception;

public class InvalidColumnNameException extends DxPgException {
    public InvalidColumnNameException(String message) {
        super(DxErrorCodes.PG_INVALID_COL_ERROR, message);
    }

    public InvalidColumnNameException(String message, Throwable cause) {
        super(DxErrorCodes.PG_INVALID_COL_ERROR, message, cause);
    }
}
