package iudx.resource.server.apiserver.util;

import java.util.List;
import java.util.regex.Pattern;

public class Constants {

  // date-time format
  public static final String APP_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]'Z'";
  public static final String APP_NAME_REGEX = "[a-zA-Z0-9._\\-]*$";

  public static final String APP_TEST_NAME = "vasanth";
  public static final String EVENT = "event";
  public static final String API_ENDPOINT = "apiEndpoint";
  public static final String API_METHOD = "method";
  public static final String ID = "id";
  public static final String RESOURCE_ID_DEFAULT =
      "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/"
          + "surat-itms-realtime-information/surat-itms-live-eta";
  public static final String RESPONSE_SIZE = "response_size";
  public static final String IDS = "ids";

  // config
  public static final String CONFIG_FILE = "config.properties";

  // NGSI-LD endpoints
  public static final String NGSILD_BASE_PATH = "/ngsi-ld/v1";
  public static final String NGSILD_ENTITIES_URL = "/entities";
  // path regex
  public static final String ENTITITES_URL_REGEX = NGSILD_ENTITIES_URL + "(.*)";
  public static final String NGSILD_TEMPORAL_URL = "/temporal/entities";
  public static final String TEMPORAL_URL_REGEX = NGSILD_TEMPORAL_URL + "(.*)";
  public static final String NGSILD_SUBSCRIPTION_URL = "/subscription";
  public static final String SUBSCRIPTION_URL_REGEX = NGSILD_SUBSCRIPTION_URL + "(.*)";
  public static final String NGSILD_POST_TEMPORAL_QUERY_PATH = "/temporal/entityOperations/query";
  public static final String TEMPORAL_POST_QUERY_URL_REGEX =
      NGSILD_POST_TEMPORAL_QUERY_PATH + "(.*)";
  public static final String NGSILD_POST_ENTITIES_QUERY_PATH = "/entityOperations/query";
  public static final String ENTITIES_POST_QUERY_URL_REGEX =
      NGSILD_POST_ENTITIES_QUERY_PATH + "(.*)";

  // Async endpoints
  public static final String STATUS = "/status";
  public static final String SEARCH = "/search";
  public static final String IUDX_ASYNC_SEARCH = "(.*)/async/search";
  public static final String IUDX_ASYNC_STATUS = "(.*)/async/status";
  public static final String IUDX_ASYNC_SEARCH_API = "/async/search";
  public static final String ASYNC = "/async";

  // IUDX management endpoints
  public static final String IUDX_MANAGEMENT_URL = "/management";
  public static final String IUDX_ADAPTOR_URL = "/ngsi-ld/v1";
  public static final String IUDX_CONSUMER_AUDIT_URL = "/consumer/audit";
  public static final String IUDX_PROVIDER_AUDIT_URL = "/provider/audit";
  public static final String IUDX_MANAGEMENT_EXCHANGE_URL = IUDX_MANAGEMENT_URL + "/exchange";
  public static final String EXCHANGE_URL_REGEX = IUDX_MANAGEMENT_EXCHANGE_URL + "(.*)";
  public static final String EXCHANGE_PATH = "/exchange";
  public static final String QUEUE_PATH = "/queue";
  public static final String IUDX_MANAGEMENT_QUEUE_URL = IUDX_MANAGEMENT_URL + "/queue";
  public static final String QUEUE_URL_REGEX = IUDX_MANAGEMENT_QUEUE_URL + "(.*)";
  public static final String BIND = "/bind";
  public static final String UNBIND = "/unbind";
  public static final String IUDX_MANAGEMENT_BIND_URL = IUDX_MANAGEMENT_URL + "/bind";
  public static final String BIND_URL_REGEX = IUDX_MANAGEMENT_BIND_URL + "(.*)";
  public static final String IUDX_MANAGEMENT_UNBIND_URL = IUDX_MANAGEMENT_URL + "/unbind";
  public static final String UNBIND_URL_REGEX = IUDX_MANAGEMENT_UNBIND_URL + "(.*)";
  public static final String IUDX_MANAGEMENT_VHOST_URL = IUDX_MANAGEMENT_URL + "/vhost";
  public static final String VHOST_URL_REGEX = IUDX_MANAGEMENT_VHOST_URL + "(.*)";
  public static final String VHOST = "/vhost";
  public static final String IUDX_MANAGEMENT_ADAPTER_URL = "/ingestion";
  public static final String ADAPTER_URL_REGEX = IUDX_MANAGEMENT_ADAPTER_URL + "(.*)";
  public static final String INGESTION_PATH = "/ingestion";
  public static final String IUDX_MANAGEMENT_RESET_PWD =
      IUDX_MANAGEMENT_URL + "/user/resetPassword";
  public static final String RESET_URL_REGEX = IUDX_MANAGEMENT_RESET_PWD + "(.*)";
  public static final String RESET_PWD = "/user/resetPassword";

  /** API Documentation endpoint */
  public static final String ROUTE_STATIC_SPEC = "/apis/spec";

  public static final String ROUTE_DOC = "/apis";
  public static final List<String> bypassEndpoint = List.of(ROUTE_STATIC_SPEC, ROUTE_DOC);
  public static final List<String> openEndPoints =
      List.of("/temporal/entities", "/entities", "/entityOperations/query");
  public static final String REVOKE_TOKEN = "/revokeToken";
  public static final String REVOKE_TOKEN_REGEX = "/admin/revokeToken" + "(.*)";
  public static final String RESOURCE_ATTRIBS = "/resourceattribute";
  public static final String UNIQUE_ATTR_REGEX = "/admin/resourceattribute";
  public static final String ADMIN = "/admin";

  public static final String MONTHLY_OVERVIEW = "/overview";
  public static final String SUMMARY_ENDPOINT = "/summary";
  public static final String INGESTION_PATH_ENTITIES = "/ingestion/entities";

  /** Accept Headers and CORS */
  public static final String MIME_APPLICATION_JSON = "application/json";

  public static final String MIME_TEXT_HTML = "text/html";

  // ngsi-ld/IUDX query paramaters
  public static final String NGSILDQUERY_ID = "id";
  public static final String NGSILDQUERY_IDPATTERN = "idpattern";
  public static final String NGSILDQUERY_TYPE = "type";
  public static final String NGSILDQUERY_COORDINATES = "coordinates";
  public static final String NGSILDQUERY_GEOMETRY = "geometry";
  public static final String NGSILDQUERY_ATTRIBUTE = "attrs";
  public static final String NGSILDQUERY_GEOREL = "georel";
  public static final String NGSILDQUERY_TIMEREL = "timerel";
  public static final String NGSILDQUERY_TIME = "time";
  public static final String NGSILDQUERY_ENDTIME = "endtime";
  public static final String NGSILDQUERY_Q = "q";
  public static final String NGSILDQUERY_GEOPROPERTY = "geoproperty";
  public static final String NGSILDQUERY_TIMEPROPERTY = "timeproperty";
  public static final String NGSILDQUERY_MAXDISTANCE = "maxdistance";
  public static final String NGSILDQUERY_MINDISTANCE = "mindistance";
  public static final String IUDXQUERY_OPTIONS = "options";
  public static final String NGSILDQUERY_ENTITIES = "entities";
  public static final String NGSILDQUERY_GEOQ = "geoQ";
  public static final String NGSILDQUERY_TEMPORALQ = "temporalQ";
  public static final String NGSILDQUERY_TIME_PROPERTY = "timeProperty";
  public static final String NGSILDQUERY_FROM = "offset";
  public static final String NGSILDQUERY_SIZE = "limit";

  // Header params
  public static final String HEADER_TOKEN = "token";
  public static final String HEADER_CSV = "csv";
  public static final String HEADER_JSON = "json";
  public static final String HEADER_PARQUET = "parquet";
  public static final String HEADER_HOST = "Host";
  public static final String HEADER_ACCEPT = "Accept";
  public static final String HEADER_CONTENT_LENGTH = "Content-Length";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_ORIGIN = "Origin";
  public static final String HEADER_REFERER = "Referer";
  public static final String HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  public static final String HEADER_OPTIONS = "options";

  public static final String COUNT_HEADER = "Count";
  public static final String PUBLIC_TOKEN = "public";
  public static final String HEADER_PUBLIC_KEY = "publicKey";
  public static final String HEADER_RESPONSE_FILE_FORMAT = "format";

  // request/response params
  public static final String CONTENT_TYPE = "content-type";
  public static final String APPLICATION_JSON = "application/json";
  public static final String SUBSCRIPTION_ID = "subscriptionID";
  public static final String EXCHANGE_ID = "exId";

  // json fields
  public static final String JSON_INSTANCEID = "instanceID";
  public static final String JSON_CONSUMER = "consumer";
  public static final String JSON_PROVIDER = "provider";
  public static final String JSON_TYPE = "type";
  public static final String JSON_NAME = "name";
  public static final String JSON_ENTITIES = "entities";
  public static final String JSON_ID = "id";
  public static final String JSON_ATTRIBUTE_FILTER = "attrs";
  public static final String JSON_NEAR = "near";
  public static final String JSON_LAT = "lat";
  public static final String JSON_LON = "lon";
  public static final String JSON_RADIUS = "radius";
  public static final String JSON_GEOMETRY = "geometry";
  public static final String JSON_COORDINATES = "coordinates";
  public static final String JSON_GEOREL = "georel";
  public static final String JSON_WITHIN = "within";
  public static final String JSON_MAXDISTANCE = "maxdistance";
  public static final String JSON_MINDISTANCE = "mindistance";
  public static final String JSON_DURING = "during";
  public static final String JSON_BETWEEN = "between";
  public static final String JSON_TIME = "time";
  public static final String JSON_ENDTIME = "endtime";
  public static final String JSON_TIMEREL = "timerel";
  public static final String JSON_ATTR_QUERY = "attr-query";
  public static final String JSON_GEOPROPERTY = "geoproperty";
  public static final String JSON_ATTRIBUTE = "attribute";
  public static final String JSON_OPERATOR = "operator";
  public static final String JSON_VALUE = "value";
  public static final String JSON_TITLE = "title";
  public static final String JSON_DETAIL = "detail";
  public static final String JSON_EXCHANGE_NAME = "exchangeName";
  public static final String JSON_QUEUE_NAME = "queueName";
  public static final String JSON_VHOST_NAME = "vHostName";
  public static final String JSON_VHOST = "vHost";
  public static final String JSON_VHOST_ID = "vhostId";
  public static final String DOMAIN = "domain";
  public static final String USERSHA = "userSha";
  public static final String JSON_ALIAS = "alias";
  public static final String JSON_COUNT = "Count";
  public static final String JSON_URL = "url";
  public static final String JSON_METHOD = "method";
  public static final String JSON_PASSWORD = "password";
  public static final String RESOURCE_SERVER = "resourceServer";
  public static final String RESOURCE_GROUP = "resourceGroup";
  public static final String RESOURCE_NAME = "resourceName";
  public static final String USER_ID = "userid";
  public static final String EXPIRY = "expiry";
  public static final String IID = "iid";
  public static final String API = "api";
  public static final String DRL = "drl";
  public static final String DID = "did";
  public static final String ENCODED_KEY = "encodedKey";
  public static final String ENCODED_CIPHER_TEXT = "encodedCipherText";
  public static final String ENCRYPTED_DATA = "encryptedData";
  public static final String JSON_EVENT_TYPE = "eventType";
  public static final String JSON_RESOURCE = "resource";

  // searchtype
  public static final String JSON_SEARCH_TYPE = "searchType";
  public static final String JSON_TEMPORAL_SEARCH = "temporalSearch_";
  public static final String JSON_GEO_SEARCH = "geoSearch_";
  public static final String JSON_RESPONSE_FILTER_SEARCH = "responseFilter_";
  public static final String JSON_ATTRIBUTE_SEARCH = "attributeSearch_";
  public static final String JSON_LATEST_SEARCH = "latestSearch_";

  // Geometry
  public static final String GEOM_POINT = "point";
  public static final String GEOM_POLYGON = "polygon";
  public static final String GEOM_LINESTRING = "linestring";

  // subscription
  public static final String SUBSCRIPTION = "subscription";
  public static final String SUB_TYPE = "subscriptionType";
  public static final String SUB_STREAMING = "streaming";
  public static final String SUB_CALLBACK = "callback";
  public static final String SUB_STREAMING_URL = "streamingURL";

  // messages (Error, Exception, messages..)
  public static final String MSG_INVALID_PARAM = "Invalid parameter in request.";
  public static final String MSG_PARAM_DECODE_ERROR = "Error while decoding query params.";
  public static final String MSG_INVALID_EXCHANGE_NAME = "Invalid exchange name";
  public static final String MSG_INVALID_QUEUE_NAME = "Invalid queue name";
  public static final String MSG_INVALID_VHOST_NAME = "Invalid vhost name";
  public static final String MSG_INVALID_NAME = "Invalid name.";
  public static final String MSG_FAILURE = "failure";
  public static final String MSG_FAILURE_NO_VHOST = "No vhosts found";
  public static final String MSG_FAILURE_VHOST_EXIST = "vHost already exists";
  public static final String MSG_FAILURE_EXCHANGE_NOT_FOUND = "Exchange not found";
  public static final String MSG_FAILURE_QUEUE_NOT_EXIST = "Queue does not exist";
  public static final String MSG_FAILURE_QUEUE_EXIST = "Queue already exists";
  public static final String MSG_EXCHANGE_EXIST = "Exchange already exists";
  public static final String MSG_SUB_TYPE_NOT_FOUND = "Subscription type not present in body";
  public static final String MSG_SUB_INVALID_TOKEN = "Invalid/no token found in header";
  public static final String MSG_BAD_QUERY = "Bad query";

  // results
  public static final String SUCCCESS = "success";

  // Validations
  public static final int VALIDATION_ID_MIN_LEN = 0;
  public static final int VALIDATION_ID_MAX_LEN = 512;
  public static final Pattern VALIDATION_ID_PATTERN =
      Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
  public static final int VALIDATION_MAX_ATTRS = 5;
  public static final int VALIDATION_MAX_DAYS_INTERVAL_ALLOWED = 10;
  public static final int VALIDATION_MAX_DAYS_INTERVAL_ALLOWED_FOR_ASYNC = 365;
  public static final int VALIDATION_COORDINATE_PRECISION_ALLOWED = 6;
  public static final int VALIDATIONS_MAX_ATTR_LENGTH = 100;
  public static final int VALIDATION_ALLOWED_COORDINATES = 10;
  public static final List<String> VALIDATION_ALLOWED_HEADERS = List.of("token", "options");

  public static final String ENCODED_PUBLIC_KEY_REGEX = "^[a-zA-Z0-9_-]{42,43}={0,2}$";

  public static final Pattern ID_DOMAIN_REGEX = Pattern.compile("^[a-zA-Z0-9.]{4,100}$");
  public static final Pattern ID_USERSHA_REGEX = Pattern.compile("^[a-zA-Z0-9.]{4,100}$");
  public static final Pattern ID_RS_REGEX = Pattern.compile("^[a-zA-Z.]{4,100}$");
  public static final Pattern ID_RG_REGEX = Pattern.compile("^[a-zA-Z-_.]{4,100}$");
  public static final Pattern ID_RN_REGEX = Pattern.compile("^[a-zA-Z0-9-_.]{4,100}$");

  public static final double VALIDATION_ALLOWED_DIST = 1000.0;
  public static final double VALIDATION_ALLOWED_DIST_FOR_ASYNC = 10000.0;
  public static final int VALIDATION_PAGINATION_LIMIT_MAX = 5000;
  public static final int VALIDATION_PAGINATION_OFFSET_MAX = 49999;
  public static final List<Object> VALIDATION_ALLOWED_GEOM =
      List.of("Point", "point", "Polygon", "polygon", "LineString", "linestring", "bbox");
  public static final List<Object> VALIDATION_ALLOWED_GEOPROPERTY = List.of("location", "Location");
  public static final List<String> VALIDATION_ALLOWED_OPERATORS =
      List.of(">", "=", "<", ">=", "<=", "==", "!=");
  public static final List<String> VALIDATION_ALLOWED_TEMPORAL_REL =
      List.of("after", "before", "during", "between");

  public static final List<String> VALIDATION_ALLOWED_TEMPORAL_REL_ASYNC =
      List.of("during", "between");

  public static final Pattern VALIDATION_Q_ATTR_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,100}$");
  public static final Pattern VALIDATION_Q_VALUE_PATTERN =
      Pattern.compile("^(?!.*--)[a-zA-Z0-9_.-]{1,100}$");

  // subscriptions queries
  public static final String CREATE_SUB_SQL =
      "INSERT INTO subscriptions"
          + "(_id,_type,queue_name,entity,expiry,dataset_name,dataset_json,user_id,"
          + "resource_group,provider_id,delegator_id,item_type) "
          + "VALUES('$1','$2','$3','$4','$5','$6','$7','$8','$9','$a','$b','$c')";

  public static final String UPDATE_SUB_SQL =
      "UPDATE subscriptions SET expiry='$1' where queue_name='$2' and entity='$3'";

  public static final String APPEND_SUB_SQL =
      "INSERT INTO subscriptions(_id,_type,queue_name,entity,expiry,dataset_name,dataset_json,user_id,"
          + "resource_group,provider_id,delegator_id,item_type) "
          + "VALUES('$1','$2','$3','$4','$5','$6','$7','$8','$9','$a','$b','$c') "
          + "ON CONFLICT(queue_name,entity) DO NOTHING";

  public static final String DELETE_SUB_SQL = "DELETE FROM subscriptions where queue_name='$1'";

  public static final String SELECT_SUB_SQL =
      "SELECT * from subscriptions where queue_name='$1' and entity='$2'";

  public static final String NO_CONTENT = "204";

  public static final String STARTT = "starttime";
  public static final String ENDT = "endtime";
  public static final String GET_ALL_QUEUE =
      "SELECT queue_name as queueName,entity,dataset_json as catItem "
          + "FROM subscriptions WHERE user_id ='$1'";
  public static final String ENTITY_QUERY =
      "select entity from subscriptions where queue_name='$0'";
  public static final String LIMITPARAM = "limit";
  public static final String OFFSETPARAM = "offset";
  public static final String TOTALHITS = "totalHits";
  public static final String EVENTTYPE_CREATED = "SUBS_CREATED";
  public static final String EVENTTYPE_DELETED = "SUBS_DELETED";
  public static final String EVENTTYPE_APPEND = "SUBS_APPEND";
  public static final String EVENTTYPE_UPDATE = "SUBS_UPDATED";
}
