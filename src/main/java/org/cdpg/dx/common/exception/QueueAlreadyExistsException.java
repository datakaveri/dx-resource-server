package org.cdpg.dx.common.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueueAlreadyExistsException extends DxSubscriptionException {
    private static final Logger LOGGER = LogManager.getLogger(QueueAlreadyExistsException.class);

    public QueueAlreadyExistsException(String message) {
        super(DxErrorCodes.SUBS_QUEUE_EXISTS, message);
        LOGGER.debug("QueueAlreadyExistsException initiated with message {}",message);
    }

    public QueueAlreadyExistsException(String message, Throwable cause) {
        super(DxErrorCodes.SUBS_QUEUE_EXISTS, message, cause);
        LOGGER.debug("QueueAlreadyExistsException initiated");
    }
}