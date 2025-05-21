package org.cdpg.dx.common.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueueBindingFailedException extends DxRabbitMqException {
    private static final Logger LOGGER = LogManager.getLogger(QueueBindingFailedException.class);

    public QueueBindingFailedException(String message) {
        super(DxErrorCodes.SUBS_QUEUE_BINDING_FAILED, message);
        LOGGER.debug("QueueBindingFailedException initiated with message {}",message);
    }

    public QueueBindingFailedException(String message, Throwable cause) {
        super(DxErrorCodes.SUBS_QUEUE_BINDING_FAILED, message, cause);
        LOGGER.debug("QueueBindingFailedException initiated");
    }
}