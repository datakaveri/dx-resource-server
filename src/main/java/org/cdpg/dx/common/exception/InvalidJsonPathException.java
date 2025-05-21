package org.cdpg.dx.common.exception;

public class InvalidJsonPathException extends DxRedisException {
    public InvalidJsonPathException(int errorCode, String message) {
        super(errorCode, message);
    }}