package org.cdpg.dx.common.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InvalidColumnNameException extends DxPgException {
    private static final Logger LOGGER = LogManager.getLogger(InvalidColumnNameException.class);

    public InvalidColumnNameException(String message) {
        super(DxErrorCodes.PG_INVALID_COL_ERROR, message);
        LOGGER.debug("InvalidColumnNameException initiated with message {}",message);
    }

    public InvalidColumnNameException(String message, Throwable cause) {
        super(DxErrorCodes.PG_INVALID_COL_ERROR, message, cause);
        LOGGER.debug("InvalidColumnNameException initiated");
    }
}
