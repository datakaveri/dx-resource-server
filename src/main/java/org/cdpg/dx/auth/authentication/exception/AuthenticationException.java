package org.cdpg.dx.auth.authentication.exception;

import org.cdpg.dx.common.exception.BaseDxException;

public class AuthenticationException extends BaseDxException {
    public AuthenticationException(String message) {
        super(/*"AUTHENTICATION_FAILED"*/100, message);
    }
}