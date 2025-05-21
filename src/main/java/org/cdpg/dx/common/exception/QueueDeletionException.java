package org.cdpg.dx.common.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueueDeletionException extends DxRabbitMqException {
    private static final Logger LOGGER = LogManager.getLogger(QueueDeletionException.class);

    public QueueDeletionException(String message) {
        super(DxErrorCodes.SUBS_QUEUE_DELETION_FAILED, message);
        LOGGER.debug("QueueDeletionException initiated with message {}",message);
    }

    public QueueDeletionException(String message, Throwable cause) {
        super(DxErrorCodes.SUBS_QUEUE_DELETION_FAILED, message, cause);
        LOGGER.debug("QueueDeletionException initiated");
    }
}