package org.cdpg.dx.database.postgres.util;

import io.vertx.pgclient.PgException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.DxPgException;
import org.cdpg.dx.common.exception.InvalidColumnNameException;
import org.cdpg.dx.common.exception.UniqueConstraintViolationException;

public class DxPgExceptionMapper {
    private static final Logger LOGGER = LogManager.getLogger(DxPgExceptionMapper.class);

    public static DxPgException from(Throwable t) {
        if (t instanceof PgException pgEx) {
            LOGGER.error("Postgres Error: {} with error code: {}", pgEx.getMessage(), pgEx.getSqlState());
            return switch (pgEx.getSqlState()) {
                case "23505" -> new UniqueConstraintViolationException(pgEx.getMessage());
                case "23503" -> new DxPgException("Foreign key violation", pgEx);
                case "23502" -> new DxPgException("Not null violation", pgEx);
                case "23514" -> new DxPgException("Check constraint violation", pgEx);

                case "42703" -> new InvalidColumnNameException(pgEx.getMessage());
                case "42601" -> new DxPgException("Syntax error", pgEx);
                case "42P01" -> new DxPgException("Undefined table", pgEx);
                case "42883" -> new DxPgException("Undefined function", pgEx);
                case "22001" -> new DxPgException("String data, right truncation", pgEx);
                case "2200G" -> new DxPgException("Specific type mismatch", pgEx);
                case "XX000", "XX001", "XX002" -> new DxPgException("Internal server error in PostgresSQL", pgEx);

                default -> new DxPgException("Postgres Error: " + pgEx.getMessage(), pgEx);
            };
        }
        return new DxPgException("Unknown DB Error", t);
    }
}