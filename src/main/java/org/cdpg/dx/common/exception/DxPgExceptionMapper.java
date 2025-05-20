package org.cdpg.dx.common.exception;

import io.vertx.pgclient.PgException;
import org.cdpg.dx.common.exception.*;

public class DxPgExceptionMapper {
    public static DxPgException from(Throwable t) {
        if (t instanceof PgException pgEx) {
            return switch (pgEx.getCode()) {
                case "42703" -> new InvalidColumnNameException(pgEx.getMessage());
                case "23505" -> new UniqueConstraintViolationException(pgEx.getMessage());
                case "23503" -> new DxPgException("Foreign key violation", pgEx);
                default -> new DxPgException("Postgres Error: " + pgEx.getMessage(), pgEx);
            };
        }
        return new DxPgException("Unknown DB Error", t);
    }
}
//
//23505: Unique violation (duplicate key)
//23503: Foreign key violation
//42703: Undefined column (invalid column name)
//23502: Not null violation
//23514: Check constraint violation
//42601: Syntax error
//42P01: Undefined table
//40001: Serialization failure (deadlock)
//22001: String data, right truncation