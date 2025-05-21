package org.cdpg.dx.common.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NoRowFoundException extends DxPgException {
    private static final Logger LOGGER = LogManager.getLogger(NoRowFoundException.class);

    public NoRowFoundException(String message) {
        super(DxErrorCodes.PG_NO_ROW_ERROR, message);
        LOGGER.debug("NoRowFoundException initiated with message {}",message);
    }

    public NoRowFoundException(String message, Throwable cause) {
        super(DxErrorCodes.PG_NO_ROW_ERROR, message, cause);
        LOGGER.debug("NoRowFoundException initiated");
    }
}