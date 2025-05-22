package org.cdpg.dx.common.exception;

public class DxRabbitMqGeneralException extends BaseDxException {

    public DxRabbitMqGeneralException(String message) {
        super(DxErrorCodes.RABBIT_MQ_ERROR, message);
    }

    public DxRabbitMqGeneralException(String message, Throwable cause) {
        super(DxErrorCodes.RABBIT_MQ_ERROR, message, cause);
    }

    public DxRabbitMqGeneralException(int code, String message) {
        super(code, message);
    }

    public DxRabbitMqGeneralException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}



