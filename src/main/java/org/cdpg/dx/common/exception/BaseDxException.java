package org.cdpg.dx.common.exception;

import static org.cdpg.dx.common.exception.DxErrorCodes.*;

import io.vertx.serviceproxy.ServiceException;

public class BaseDxException extends ServiceException {


    public BaseDxException(int failureCode, String message) {
        super(failureCode, message);
    }

    public BaseDxException(int failureCode, String message, Throwable cause) {
        super(failureCode, message);
        initCause(cause);
    }

    public BaseDxException(String message) {
        super(DEFAULT_CODE, message);
    }

    public static BaseDxException from(Throwable t) {
        if (t instanceof BaseDxException) {
            return (BaseDxException) t;
        }

        if (t instanceof ServiceException se) {
            return fromServiceException(se);
        }

        return new BaseDxException(DEFAULT_CODE, t.getMessage(), t);
    }

    private static BaseDxException fromServiceException(ServiceException se) {
        return switch (se.failureCode()) {
            case PG_NO_ROW_ERROR -> new NoRowFoundException(se.getMessage(), se);
            case PG_INVALID_COL_ERROR -> new InvalidColumnNameException(se.getMessage(), se);
            case PG_UNIQUE_CONSTRAINT_VIOLATION_ERROR ->  new UniqueConstraintViolationException(se.getMessage(), se);
            case PG_ERROR -> new DxPgException(se.getMessage(), se);
            // You can add other subclasses here as needed
            default -> new BaseDxException(se.failureCode(), se.getMessage(), se);
        };
    }
}
