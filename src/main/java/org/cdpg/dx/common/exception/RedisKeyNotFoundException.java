package org.cdpg.dx.common.exception;

public class RedisKeyNotFoundException extends DxRedisException {
    public RedisKeyNotFoundException(int errorCode, String message) {
        super(errorCode, message);
    }}