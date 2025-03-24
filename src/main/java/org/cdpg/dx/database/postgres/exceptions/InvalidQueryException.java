package org.cdpg.dx.database.postgres.exceptions;

public class InvalidQueryException extends DxDatabaseException {
    public InvalidQueryException(String message) {
        super("INVALID_QUERY", message);
    }
}
