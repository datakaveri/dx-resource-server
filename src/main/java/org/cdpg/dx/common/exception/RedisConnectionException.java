package org.cdpg.dx.common.exception;

public class RedisConnectionException extends DxRedisException {
    public RedisConnectionException(int errorCode, String message) {
        super(errorCode, message);
    }
}