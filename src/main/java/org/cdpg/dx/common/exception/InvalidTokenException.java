package org.cdpg.dx.common.exception;

public class InvalidTokenException extends DxException {
    public InvalidTokenException(String message) {
        super("INVALID_TOKEN", message);
    }
}