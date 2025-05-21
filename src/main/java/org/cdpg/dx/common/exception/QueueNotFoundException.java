package org.cdpg.dx.common.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueueNotFoundException extends DxRabbitMqException {
    private static final Logger LOGGER = LogManager.getLogger(QueueNotFoundException.class);

    public QueueNotFoundException(String message) {
        super(DxErrorCodes.SUBS_QUEUE_NOT_FOUND, message);
        LOGGER.debug("QueueNotFoundException initiated with message {}",message);
    }

    public QueueNotFoundException(String message, Throwable cause) {
        super(DxErrorCodes.SUBS_QUEUE_NOT_FOUND, message, cause);
        LOGGER.debug("QueueNotFoundException initiated");
    }
}