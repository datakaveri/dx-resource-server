package org.cdpg.dx.common.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueueRegistrationFailedException extends DxRabbitMqException {
    private static final Logger LOGGER = LogManager.getLogger(QueueRegistrationFailedException.class);

    public QueueRegistrationFailedException(String message) {
        super(DxErrorCodes.SUBS_QUEUE_REGISTRATION_FAILED, message);
        LOGGER.debug("QueueRegistrationFailedException initiated with message {}",message);
    }

    public QueueRegistrationFailedException(String message, Throwable cause) {
        super(DxErrorCodes.SUBS_QUEUE_REGISTRATION_FAILED, message, cause);
        LOGGER.debug("QueueRegistrationFailedException initiated");
    }
}