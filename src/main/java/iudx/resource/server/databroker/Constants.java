package iudx.resource.server.databroker;

public class Constants {

  // Registration response fields used for construction JSON
  public static final String APIKEY = "apiKey";
  public static final String ALLOW = ".*";
  public static final String ADAPTOR_ID_NOT_PROVIDED =
      "AdaptorID / Exchange not provided in request";
  public static final String AUTO_DELETE = "auto_delete";
  public static final String ALL_NOT_FOUND = "Queue/Exchange/Routing Key does not exist";
  public static final String APIKEY_TEST_EXAMPLE = "123456";
  public static final String ALLOW_ROUTING_KEY = "/.*";

  
  public static final String BAD_REQUEST =
      "Bad request : insufficient request data to register adaptor";
  public static final String BROKER_IP = "68.183.80.248";
  public static final String BROKER_PORT = "5672";
  public static final String CHECK_CREDENTIALS =
      "Something went wrong while creating user using mgmt API. Check credentials";
  public static final String CONFIGURE = "configure";
  public static final String CONSUMER = "consumer";
  public static final String CONSUMER_TEST_EXAMPLE = "vasanth.rajaraman@rbccps.org";

  public static final String DETAILS = "details";
  public static final String DATA_ISSUE = ".dataIssue";
  public static final String DOWNSTREAM_ISSUE = ".downstreamIssue";
  public static final String DENY = "";
  public static final String DETAIL = "detail";
  public static final String DURABLE = "durable";

  public static final String ENTITIES = "entities";
  public static final String EXCHANGE_FOUND = "Exchange found";
  public static final String EXCHANGE_EXISTS = "Exchange already exists";
  public static final String EXCHANGE_EXISTS_WITH_DIFFERENT_PROPERTIES =
      "Exchange already exists with different properties";
  public static final String EXCHANGE_LIST_ERROR = "Listing of Exchange failed";
  public static final String EXCHANGE_DELETE_ERROR = "Deletion of Exchange failed";
  public static final String EXCHANGE_CREATE_ERROR = "Creation of Exchange failed";
  public static final String EXCHANGE = "exchange";
  public static final String EXCHANGE_NAME = "exchangeName";
  public static final String ERROR = "error";
  public static final String EXCHANGE_DECLARATION_ERROR = "something wrong in exchange declaration";
  public static final String EXCHANGE_NOT_FOUND = "Exchange not found";
  public static final String EXCHANGE_TYPE = "topic";
  public static final boolean EXCHANGE_DURABLE_TRUE = true;
  public static final boolean EXCHANGE_AUTODELETE_FALSE = false;

  public static final String FAILURE = "failure";

  public static final String HEARTBEAT = ".heartbeat";

  public static final String ID = "id";
  public static final String ID_NOT_PROVIDED = "id not provided in adaptor registration";
  public static final String INVALID_ID = "Invalid id field";
  public static final String ID_ALIAS_TEST_EXAMPLE_2 = "flood-sensor";
  public static final String ID_ALIAS_TEST_EXAMPLE_1 = "aqm-bosch-climo";
  public static final String ID_TEST_EXAMPLE_1 =
      "rbccps.org/e73ed7f5b7950f8b3b42f4bd14eade5c/aqm-bosch-climo";
  public static final String ID_TEST_EXAMPLE_2 =
      "rbccps.org/e73ed7f5b7950f8b3b42f4bd14eade5c/flood-sensor";
  public static final String ID_TEST_EXAMPLE =
      "rbccps.org/e73ed7f5b7950f8b3b42f4bd14eade5c/aqm-bosch-climo";

  public static final String NAME = "name";
  public static final String NETWORK_ISSUE = "Network Issue";
  public static final String NONE = "None";

  public static final String PASSWORD = "password";
  public static final int PASSWORD_LENGTH = 16;

  public static final String QUEUE = "queue";
  public static final String QUEUE_BIND_ERROR = "error in queue binding with adaptor";
  public static final String QUEUE_DOES_NOT_EXISTS = "Queue does not exist";
  public static final String QUEUE_ALREADY_EXISTS = "Queue already exists";
  public static final String QUEUE_ALREADY_EXISTS_WITH_DIFFERENT_PROPERTIES =
      "Queue already exists with different properties";
  public static final String QUEUE_DATA = "database";
  public static final String QUEUE_ADAPTOR_LOGS = "adaptorLogs";
  public static final String QUEUE_LIST_ERROR = "Listing of Queue failed";
  public static final String QUEUE_DELETE_ERROR = "Deletion of Queue failed";
  public static final String QUEUE_CREATE_ERROR = "Creation of Queue failed";
  public static final String QUEUE_EXCHANGE_NOT_FOUND = "Queue/Exchange does not exist";
  public static final String QUEUE_NAME = "queueName";

  public static final String READ = "read";
  public static final String ROUTING_KEY = "routingKey";

  public static final String STATUS = "status";
  public static final String SUCCESS = "success";
  public static final String SHA_USER_NAME = "shaUsername";
  public static final String SUBSCRIPTION_ID = "subscriptionID";
  public static final String STREAMING_URL = "streamingURL";
  
  public static final String TOPIC_PERMISSION = "topic_permissions";
  public static final String TOPIC_PERMISSION_SET_SUCCESS = "topic permission set";
  public static final String TOPIC_PERMISSION_ALREADY_SET = "topic permission already set";
  public static final String TOPIC_PERMISSION_SET_ERROR = "Error in setting Topic permissions";
  public static final String TAGS = "tags";
  public static final String TYPE = "type";
  public static final String TITLE = "title";

  public static final String USER_NAME = "username";
  public static final String USER_CREATED = "User created";
  public static final String USER_EXISTS = "User exists";
  public static final String USER_ALREADY_EXISTS = "User already exists";
  public static final String USER_NAME_NOT_PROVIDED =
      "userName not provided in adaptor registration";
  public static final String USER_CREATION_ERROR = "User creation failed";
  public static final String USER_NAME_TEST_EXAMPLE = "rbccps.org/e73ed7f5b7950f8b3b42f4bd14eade5c";

  public static final String VHOST = "vHost";
  public static final String VHOST_IUDX = "IUDX";
  public static final String VHOST_PERMISSIONS_WRITE = "write permission set";
  public static final String VHOST_PERMISSION_SET_ERROR = "Error in setting vHost permissions";
  public static final String VHOST_PERMISSIONS = "vhostPermissions";
  public static final String VHOST_PERMISSIONS_FAILURE = "Error in setting vhostPermissions";
  public static final String VHOST_NOT_FOUND = "No vhosts found";
  public static final String VHOST_ALREADY_EXISTS = "vHost already exists";
  public static final String VHOST_LIST_ERROR = "Listing of vHost failed";
  public static final String VHOST_DELETE_ERROR = "Deletion of vHost failed";
  public static final String VHOST_CREATE_ERROR = "Creation of vHost failed";

  public static final String WRITE = "write";

  public static final String X_MESSAGE_TTL_NAME = "x-message-ttl";
  public static final String X_MAXLENGTH_NAME = "x-max-length";
  public static final String X_QUEUE_MODE_NAME = "x-queue-mode";
  public static final long X_MESSAGE_TTL_VALUE = 86400000; // 24hours
  public static final int X_MAXLENGTH_VALUE = 100;
  public static final String X_QUEUE_MODE_VALUE = "lazy";
  
}
