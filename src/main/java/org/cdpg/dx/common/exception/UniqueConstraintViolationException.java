package org.cdpg.dx.common.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UniqueConstraintViolationException extends DxPgException {
    private static final Logger LOGGER = LogManager.getLogger(UniqueConstraintViolationException.class);

    public UniqueConstraintViolationException(String message) {
        super(DxErrorCodes.PG_UNIQUE_CONSTRAINT_VIOLATION_ERROR, message);
        LOGGER.debug("UniqueConstraintViolationException initiated with message {}",message);
    }

    public UniqueConstraintViolationException(String message, Throwable cause) {
        super(DxErrorCodes.PG_UNIQUE_CONSTRAINT_VIOLATION_ERROR, message, cause);
        LOGGER.debug("UniqueConstraintViolationException initiated");
    }
}
