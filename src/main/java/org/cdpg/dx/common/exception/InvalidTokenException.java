package org.cdpg.dx.common.exception;

public class InvalidTokenException extends BaseDxException {
    public InvalidTokenException(String message) {
        super(/*"INVALID_TOKEN"*/10, message);
    }
}