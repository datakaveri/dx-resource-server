package org.cdpg.dx.auth.authentication.exception;

import org.cdpg.dx.common.exception.DxException;

public class AuthenticationException extends DxException {
    public AuthenticationException(String message) {
        super("AUTHENTICATION_FAILED", message);
    }
}