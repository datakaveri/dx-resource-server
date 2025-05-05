package org.cdpg.dx.auth.authorization.exception;

import org.cdpg.dx.common.exception.DxException;

public class AuthorizationException extends DxException {
    public AuthorizationException(String message) {
        super("AUTHORIZATION_FAILED", message);
    }
}