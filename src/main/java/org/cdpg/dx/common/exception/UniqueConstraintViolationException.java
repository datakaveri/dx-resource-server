package org.cdpg.dx.common.exception;

public class UniqueConstraintViolationException extends DxPgException {
    public UniqueConstraintViolationException(String message) {
        super(DxErrorCodes.PG_UNIQUE_CONSTRAINT_VIOLATION_ERROR, message);
    }

    public UniqueConstraintViolationException(String message, Throwable cause) {
        super(DxErrorCodes.PG_UNIQUE_CONSTRAINT_VIOLATION_ERROR, message, cause);
    }
}
