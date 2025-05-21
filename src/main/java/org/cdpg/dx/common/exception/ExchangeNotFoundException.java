package org.cdpg.dx.common.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExchangeNotFoundException extends DxRabbitMqException {
    private static final Logger LOGGER = LogManager.getLogger(ExchangeNotFoundException.class);

    public ExchangeNotFoundException(String message) {
        super(DxErrorCodes.SUBS_EXCHANGE_NOT_FOUND, message);
        LOGGER.debug("ExchangeNotFoundException initiated with message {}",message);
    }

    public ExchangeNotFoundException(String message, Throwable cause) {
        super(DxErrorCodes.SUBS_EXCHANGE_NOT_FOUND, message, cause);
        LOGGER.debug("ExchangeNotFoundException initiated");
    }
}