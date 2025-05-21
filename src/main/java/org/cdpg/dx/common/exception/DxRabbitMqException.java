package org.cdpg.dx.common.exception;

public class DxRabbitMqException extends BaseDxException {

    public DxRabbitMqException(String message) {
        super(DxErrorCodes.RABBIT_MQ_ERROR, message);
    }

    public DxRabbitMqException(String message, Throwable cause) {
        super(DxErrorCodes.RABBIT_MQ_ERROR, message, cause);
    }

    public DxRabbitMqException(int code, String message) {
        super(code, message);
    }

    public DxRabbitMqException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}



