package org.cdpg.dx.common.exception;

public class DxErrorCodes {
    public static final int DEFAULT_CODE = 10000;

    public static final int VALIDATION_ERROR = 10001;

    public static final int PG_ERROR = 11000;
    public static final int PG_NO_ROW_ERROR = 11001;
    public static final int PG_INVALID_COL_ERROR = 11002;
    public static final int PG_UNIQUE_CONSTRAINT_VIOLATION_ERROR = 11003;
    public static final int NOT_FOUND = 10002;
    public static final int UNAUTHORIZED = 10003;
    public static final int INTERNAL_ERROR = 10004;
}
