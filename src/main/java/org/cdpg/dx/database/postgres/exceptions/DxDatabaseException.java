package org.cdpg.dx.database.postgres.exceptions;

import org.cdpg.dx.common.exception.DxException;

public class DxDatabaseException extends DxException {
    public DxDatabaseException(String errorCode, String message) {
        super(errorCode, message);
    }
}
