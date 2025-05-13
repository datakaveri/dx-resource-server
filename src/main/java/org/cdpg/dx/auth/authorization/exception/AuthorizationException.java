package org.cdpg.dx.auth.authorization.exception;

import org.cdpg.dx.common.exception.BaseDxException;

public class AuthorizationException extends BaseDxException {
    public AuthorizationException(String message) {
        super(/*"AUTHORIZATION_FAILED"*/100, message);
    }
}