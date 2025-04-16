package org.cdpg.dx.databroker.util;

public class Constants {
    public static final String REQUEST_GET = "GET";
    public static final String REQUEST_POST = "POST";
    public static final String REQUEST_PUT = "PUT";
    public static final String REQUEST_DELETE = "DELETE";
    public static String UNIQUE_ATTR_Q = "rs-unique-attributes";
    public static String ASYNC_QUERY_Q = "rs-async-query";
    public static String TOKEN_INVALID_Q = "rs-invalid-sub";
    public static final String QUEUE_NAME = "queueName";
    public static final String PASSWORD = "password";
    public static final String TAGS = "tags";
    public static final String NONE = "None";
    public static final String CONFIGURE = "configure";
    public static final String DENY = "";
    public static final String READ = "read";
    public static final String WRITE = "write";
    public static final String QUEUE = "queue";
    public static final String X_MESSAGE_TTL_NAME = "x-message-ttl";
    public static final String X_MAXLENGTH_NAME = "x-max-length";
    public static final String X_QUEUE_MODE_NAME = "x-queue-mode";
    public static final long X_MESSAGE_TTL_VALUE = 86400000; // 24hours
    public static final int X_MAXLENGTH_VALUE = 10000;
    public static final String X_QUEUE_MODE_VALUE = "lazy";
    public static final String X_QUEUE_TYPE = "durable";
    public static final String X_QUEUE_ARGUMENTS = "arguments";
    public static final String DATA_WILDCARD_ROUTINGKEY = "/.*";
    public static final String EXCHANGE_NAME = "exchangeName";
    public static final String EXCHANGE = "exchange";
    public static final String USER_NAME = "username";
    public static final String APIKEY = "apiKey";
    public static final String ID = "id";
    public static final String SUCCESS = "success";
    public static final String TYPE = "type";
    public static final String TITLE = "title";
    public static final String STATUS = "status";
    public static final String DETAIL = "detail";
    public static final int INTERNAL_ERROR_CODE = 500;
    public static final String QUEUE_DELETE_ERROR = "Deletion of Queue failed";
    public static final String BINDING_FAILED = "Binding failed";
    public static final int BAD_REQUEST_CODE = 400;
    public static final String QUEUE_DOES_NOT_EXISTS = "Queue does not exist";
    public static final String FAILURE = "failure";
    public static final String QUEUE_LIST_ERROR = "Listing of Queue failed";
    public static final String DETAILS = "details";
    public static final String USER_CREATION_ERROR = "User creation failed";
    public static final String DATABASE_READ_SUCCESS = "Read Database Success";
    public static final String NETWORK_ISSUE = "Network Issue";
    public static final String CHECK_CREDENTIALS =
            "Something went wrong while creating user using mgmt API. Check credentials";
    public static final String VHOST_PERMISSIONS_WRITE = "write permission set";
    public static final String VHOST_PERMISSION_SET_ERROR = "Error in setting vHost permissions";
    public static final String QUEUE_ALREADY_EXISTS = "Queue already exists";
    public static final String QUEUE_ALREADY_EXISTS_WITH_DIFFERENT_PROPERTIES =
            "Queue already exists with different properties";
    public static final String QUEUE_CREATE_ERROR = "Creation of Queue failed";
    public static final String QUEUE_EXCHANGE_NOT_FOUND = "Queue/Exchange does not exist";
    public static final String QUEUE_BIND_ERROR = "error in queue binding with adaptor";
    public static final String BAD_REQUEST_DATA = "Bad Request data";
    public static final int SUCCESS_CODE = 200;
    public static final String URL = "URL";
    public static final String VHOST = "vHost";
    public static final String PORT = "port";
    public static final String VHOST_PERMISSIONS = "vhostPermissions";
    public static final String API_KEY_MESSAGE =
            "Use the apiKey returned on registration, if lost please use /resetPassword API";
    public static final String ENTITIES = "entities";
    public static final String EXCHANGE_DELETE_ERROR = "Deletion of Exchange failed";
    public static final String AUTO_DELETE = "auto_delete";
    public static final String EXCHANGE_TYPE = "topic";
    public static final String DURABLE = "durable";
    public static final String EXCHANGE_FOUND = "Exchange found";
    public static final String EXCHANGE_NOT_FOUND = "Exchange not found";
    public static final String EXCHANGE_CREATE_ERROR = "Creation of Exchange failed";
    public static final String DATABASE_QUEUE = "database";
    public static final String QUEUE_SUBS = "subscriptions-monitoring";
    public static final String REDIS_LATEST_QUEUE = "redis-latest";
    public static final String EXCHANGE_EXISTS = "Exchange already exists";
    public static final String EXCHANGE_EXISTS_WITH_DIFFERENT_PROPERTIES =
            "Exchange already exists with different properties";
    public static final String USER_ID = "userid";
}
