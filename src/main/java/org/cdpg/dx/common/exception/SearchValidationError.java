package org.cdpg.dx.common.exception;

public class SearchValidationError extends DxSearchException {

    public SearchValidationError(String message) {
        super(DxErrorCodes.SEARCH_ERROR, message);
    }

    public SearchValidationError(String message, Throwable cause) {
        super(DxErrorCodes.SEARCH_ERROR, message, cause);
    }

    public SearchValidationError(int code, String message) {
        super(code, message);
    }

    public SearchValidationError(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}



