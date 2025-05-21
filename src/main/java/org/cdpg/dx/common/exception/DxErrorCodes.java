package org.cdpg.dx.common.exception;

public class DxErrorCodes {
    public static final int DEFAULT_CODE = 10000;

    public static final int VALIDATION_ERROR = 10001;

    public static final int PG_ERROR = 11000;
    public static final int PG_NO_ROW_ERROR = 11001;
    public static final int PG_INVALID_COL_ERROR = 11002;
    public static final int PG_UNIQUE_CONSTRAINT_VIOLATION_ERROR = 11003;
    public static final int NOT_FOUND = 10002;

    public static final int SUBS_ERROR = 22000;
    public static final int SUBS_QUEUE_EXISTS = 22001;
    public static final int SUBS_QUEUE_REGISTRATION_FAILED = 22002;
    public static final int SUBS_QUEUE_BINDING_FAILED = 22003;
    public static final int SUBS_QUEUE_NOT_FOUND = 22004;
    public static final int SUBS_QUEUE_DELETION_FAILED = 22005;
    public static final int SUBS_EXCHANGE_NOT_FOUND = 22006;

    public static final int RABBIT_MQ_ERROR = 44000;
    public static final int RABBIT_MQ_QUEUE_EXISTS = 44001;
    public static final int RABBIT_MQ_QUEUE_REGISTRATION_FAILED = 44002;
    public static final int RABBIT_MQ_QUEUE_BINDING_FAILED = 44003;

    public static final int REDIS_ERROR = 44000;
    public static final int KEY_NOT_FOUND = 5001;
    public static final int CONNECTION_ERROR = 5002;
    public static final int INVALID_JSON_PATH = 5003;
    public static final int OPERATION_FAILED = 5004;
    public static final int INVALID_RESPONSE = 5005;
    public static final int REDIS_TIMEOUT = 5006;

    public static final int UNAUTHORIZED = 10003;
    public static final int INTERNAL_ERROR = 10004;
}
