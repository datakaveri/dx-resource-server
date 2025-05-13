package org.cdpg.dx.database.postgres.util;

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