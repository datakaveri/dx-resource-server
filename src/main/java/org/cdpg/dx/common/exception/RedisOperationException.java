package org.cdpg.dx.common.exception;

public class RedisOperationException extends DxRedisException {
    public RedisOperationException(int errorCode, String message) {
        super(errorCode, message);
    }}