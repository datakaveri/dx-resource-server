package org.cdpg.dx.database.elastic.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Constants {

  /* General Purpose */
  public static final String SEARCH_TYPE = "searchType";
  public static final String ID = "id";
  public static final String PROD_INSTANCE = "production";
  public static final String TEST_INSTANCE = "test";

  /* Attribute */
  public static final String ATTRIBUTE_QUERY_KEY = "attr-query";
  public static final String ATTRIBUTE_KEY = "attribute";
  public static final String OPERATOR = "operator";
  public static final String VALUE = "value";
  public static final String VALUE_LOWER = "valueLower";
  public static final String VALUE_UPPER = "valueUpper";
  public static final String GREATER_THAN_OP = ">";
  public static final String LESS_THAN_OP = "<";
  public static final String GREATER_THAN_EQ_OP = ">=";
  public static final String LESS_THAN_EQ_OP = "<=";
  public static final String EQUAL_OP = "==";
  public static final String NOT_EQUAL_OP = "!=";
  public static final String BETWEEN_OP = "<==>";

  public static final String DATA_SAMPLE = "dataSample";
  public static final String DATA_DESCRIPTOR = "dataDescriptor";
  public static final String LABEL = "label";
  public static final String DOC_ID = "_id";
  public static final String KEY = "key";
  public static final String ERROR_INVALID_PARAMETER = "Incorrect/missing query parameters";

  /* Geo-Spatial */
  public static final String LAT = "lat";
  public static final String LON = "lon";
  public static final String GEOMETRY = "geometry";
  public static final String GEOREL = "georel";
  public static final String WITHIN = "within";
  public static final String POLYGON = "polygon";
  public static final String LINESTRING = "linestring";
  public static final String GEO_PROPERTY = "geoproperty";
  public static final String BBOX = "bbox";
  public static final String GEO_RADIUS = "radius";

  /* Temporal */
  public static final String REQ_TIMEREL = "timerel";
  public static final String TIME_KEY = "time";
  public static final String END_TIME = "endtime";
  public static final String DURING = "during";
  public static final String BETWEEN = "between";
  public static final String AFTER = "after";
  public static final String BEFORE = "before";
  public static final String TEQUALS = "tequals";
  public static final String TIME_LIMIT = "timeLimit";

  /**
   * Search type regex.
   */
  public static final String TAGSEARCH_REGEX = "(.*)tagsSearch(.*)";

  public static final String TEXTSEARCH_REGEX = "(.*)textSearch(.*)";
  public static final String ATTRIBUTE_SEARCH_REGEX = "(.*)attributeSearch(.*)";
  public static final String GEOSEARCH_REGEX = "(.*)geoSearch(.*)";
  public static final String RESPONSE_FILTER_GEO = "responseFilter_geoSearch_";
  public static final String RESPONSE_FILTER_REGEX = "(.*)responseFilter(.*)";

  /**
   * DB Query related.
   */
  public static final String MATCH_KEY = "match";

  public static final String TERMS_KEY = "terms";
  public static final String STRING_QUERY_KEY = "query_string";
  public static final String FROM = "from";
  public static final String KEYWORD_KEY = ".keyword";
  public static final String DEVICEID_KEY = "deviceId";
  public static final String TAG_AQM = "aqm";
  public static final String DESCRIPTION_ATTR = "description";
  public static final String ACCESS_POLICY = "accessPolicy";

  /**
   * OldElasticClient search types.
   */
  public static final String DOC_IDS_ONLY = "DOCIDS";

  public static final String SOURCE_ONLY = "SOURCE";
  public static final String DATASET = "DATASET";
  public static final String FORWARD_SLASH = "/";
  public static final String WILDCARD_KEY = "wildcard";
  public static final String AGGREGATION_ONLY = "AGGREGATION";
  public static final String RATING_AGGREGATION_ONLY = "R_AGGREGATION";
  public static final String TYPE_KEYWORD = "type.keyword";
  public static final String WORD_VECTOR_KEY = "_word_vector";
  public static final String SOURCE_AND_ID = "SOURCE_ID";
  public static final String SOURCE_AND_ID_GEOQUERY = "SOURCE_ID_GEOQUERY";
  public static final String RESOURCE_AGGREGATION_ONLY = "RESOURCE_AGGREGATION";
  public static final String PROVIDER_AGGREGATION_ONLY = "PROVIDER_AGGREGATION";

  public static final String RESPONSE_ATTRS = "attrs";


  /**
   * Some queries.
   */
  public static final String LIST_INSTANCES_QUERY =
          "{\"size\": 0, \"aggs\":"
                  + "{\"results\": {\"terms\":"
                  + "{\"field\":instances.keyword,"
                  + "\"size\": 10000}}}}";

  public static final String LIST_INSTANCE_TAGS_QUERY =
          "{\"query\": {\"bool\": {\"filter\": {\"term\": {\"instance.keyword\": \"$1\"}}}},"
                  + "\"aggs\":"
                  + "{\"results\": {\"terms\":"
                  + "{\"field\":\"tags.keyword\","
                  + "\"size\": $size}}}}";
  public static final String LIST_TAGS_QUERY =
          "{ \"aggs\":"
                  + "{\"results\": {\"terms\":"
                  + "{\"field\":\"tags.keyword\","
                  + "\"size\": $size}}}}";
  public static final String LIST_INSTANCE_TYPES_QUERY =
          "{\"query\": {\"bool\": {\"filter\": [ {\"match\": {\"type\": \"$1\"}},"
                  + "{\"term\": {\"instance.keyword\": \"$2\"}}]}},"
                  + "\"aggs\": {\"results\": {\"terms\": {\"field\": \"id.keyword\", \"size\": $size}}}}";
  public static final String LIST_TYPES_QUERY =
          "{\"query\": {\"bool\": {\"filter\": [ {\"match\": {\"type\": \"$1\"}} ]}},"
                  + "\"aggs\": {\"results\": {\"terms\": {\"field\": \"id.keyword\", \"size\": $size}}}}";
  public static final String GEO_SHAPE_QUERY =
          "{ \"geo_shape\": { \"$4\": { \"shape\": { \"type\": \"$1\", \"coordinates\": $2 },"
                  + " \"relation\": \"$3\" } } }";
  public static final String TEXT_QUERY = "{\"query_string\":{\"query\":\"$1\"}}";
  public static final String GET_DOC_QUERY =
          "{\"_source\":[$2],\"query\":{\"term\":{\"id.keyword\":\"$1\"}}}";

  public static final String GET_INSTANCE_CASE_INSENSITIVE_QUERY =
          "{\"_source\":[$2], \"query\": {\"match\": {\"id\": \"$1\"}}}";
  public static final String GET_ASSOCIATED_ID_QUERY =
          "{\"query\":{\"bool\":{\"should\":[{"
                  + "\"match\":{\"id.keyword\":\"$1\"}},{"
                  + "\"match\":{\"resourceGroup.keyword\":\"$2\"}}],"
                  + "\"minimum_should_match\":1}},"
                  + "\"_source\":[\"id\"]}";
  public static final String GET_RDOC_QUERY =
          "{\"_source\":[$2],\"query\":{\"bool\": {\"must\": "
                  + "[ { \"match\": {\"ratingID.keyword\":\"$1\"} } ],"
                  + "\"must_not\": [ { \"match\": {\"status\": \"denied\"} } ] } } }";
  public static final String CHECK_MDOC_QUERY =
          "{\"_source\":[$2],\"query\":{\"bool\": {\"must\": [ { \"match\": "
                  + "{\"id.keyword\":\"$1\"} } ] } } }";
  public static final String CHECK_MDOC_QUERY_INSTANCE =
          "{\"_source\":[$2],\"query\":{\"bool\": {\"must\": [ { \"match\": "
                  + "{\"instanceId.keyword\":\"$1\"} } ] } } }";
  public static final String GET_MLAYER_INSTANCE_QUERY =
          "{\"query\":{\"match\":{\"instanceId.keyword\": \"$1\"}},\"_source\":"
                  + "{\"includes\": [\"instanceId\",\"name\",\"cover\",\"icon\","
                  + "\"logo\",\"coordinates\"]}}";
  public static final String GET_ALL_MLAYER_INSTANCE_QUERY =
          "{\"query\": {\"match_all\": {}},\"_source\":{\"includes\": "
                  + "[\"instanceId\",\"name\",\"cover\",\"icon\",\"logo\","
                  + "\"coordinates\" ]},\"size\": $0,\"from\": $2}";
  public static final String GET_MLAYER_DOMAIN_QUERY =
          "{\"query\": {\"match\":{\"domainId.keyword\": \"$1\"}},\"_source\":{\"includes\": "
                  + "[\"domainId\",\"description\",\"icon\",\"label\",\"name\"]},\"size\": 10000}";
  public static final String GET_ALL_MLAYER_DOMAIN_QUERY =
          "{\"query\": {\"match_all\": {}},\"_source\":{\"includes\": "
                  + "[\"domainId\",\"description\",\"icon\",\"label\",\"name\"]},"
                  + "\"size\": $0,\"from\": $2}";
  public static final String CHECK_MDOC_QUERY_DOMAIN =
          "{\"_source\":[$2],\"query\":{\"bool\": {\"must\": [ { \"match\": "
                  + "{\"domainId.keyword\":\"$1\"} } ] } } }";
  public static final String GET_MLAYER_PROVIDERS_QUERY =
          "{\"query\": {\"match\": {\"type.keyword\": \"iudx:Provider\"}},\"_source\": "
                  + "{\"includes\": [\"id\",\"description\"]},\"size\": $0,\"from\": $1}";
  public static final String GET_MLAYER_GEOQUERY =
          "{ \"query\": { \"bool\": { \"minimum_should_match\": 1, \"should\": [$1]}},\"_source\": "
                  + "{\"includes\": [\"id\",\"location\",\"instance\",\"label\"] }}";
  public static final String GET_MLAYER_BOOL_GEOQUERY =
          "{\"bool\": {\"should\": [{ \"match\": { \"type.keyword\": \"iudx:Resource\" } },"
                  + "{ \"match\": { \"type.keyword\": \"iudx:ResourceGroup\" } }],\"must\": "
                  + "[{\"match\": {\"instance.keyword\": \"$2\"}},{\"match\": {\"id.keyword\": "
                  + "\"$3\"}}]}}";
  public static final String GET_ALL_MLAYER_INSTANCES =
          "{\"size\": 10000,\"_source\": [\"name\", \"icon\"]}";
  public static final String GET_MLAYER_ALL_DATASETS =
          "{\"query\":{\"bool\":{\"must\":{\"terms\":{\"type.keyword\": [\"iudx:Provider\", "
                  + "\"iudx:COS\", \"iudx:ResourceGroup\", \"iudx:Resource\"]}}}},\"_source\""
                  + ":{\"includes\": "
                  + "[\"type\",\"id\",\"label\",\"accessPolicy\",\"tags\",\"instance\","
                  + "\"provider\", \"resourceServerRegURL\",\"description\" ,\"cosURL\", "
                  + "\"cos\", \"resourceGroup\", \"resourceType\"]},\"size\": 10000}";
  public static final String GET_ALL_DATASETS_BY_RS_GRP =
          "{\"size\":10000,\"query\":{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"match\":"
                  + "{\"type.keyword\":\"iudx:ResourceGroup\"}}]}}]}}}";
  public static final String GET_ALL_DATASETS_BY_FIELDS =
          "{\"query\":{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":"
                  + "{\"type.keyword\":\"iudx:ResourceGroup\"}}";
  public static final String GET_ALL_DATASETS_BY_FIELD_SOURCE =
          "]}},"
                  + "{\"bool\":{\"must\":[{\"terms\":{\"type.keyword\":"
                  + "[\"iudx:Provider\",\"iudx:COS\"]}}]}}]}},\"_source\":"
                  + "{\"includes\":[\"type\",\"id\",\"label\",\"accessPolicy\","
                  + "\"tags\",\"instance\",\"provider\",\"resourceServerRegURL\","
                  + "\"description\",\"cosURL\",\"cos\",\"resourceGroup\"]},"
                  + "\"size\":10000}";
  public static final String GET_MLAYER_DATASET =
          "{\"query\":{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":{\"id.keyword\":\"$1\"}}"
                  + ",{\"match\": {\"type.keyword\": \"iudx:ResourceGroup\"}}]}},{\"bool\":{\"must\":"
                  + "[{\"match\":{\"id.keyword\": \"$2\"}},{\"match\":{\"type.keyword\": "
                  + "\"iudx:Provider\"}}]}},{\"bool\":{\"must\":[{\"match\":{\"resourceGroup.keyword\""
                  + ":\"$1\"}},{\"match\":{\"type.keyword\":\"iudx:Resource\"}}]}},{\"bool\":{\"must\":"
                  + "[{\"match\":{\"id.keyword\": \"$3\"}},{\"match\":{\"type.keyword\": \"iudx:COS\""
                  + "}}]}}]}},\"_source\":{\"includes\":[\"resourceServer\", \"id\", \"type\","
                  + " \"apdURL\","
                  + " \"label\", \"description\", \"instance\", \"accessPolicy\",\"cosURL\","
                  + " \"dataSample\","
                  + " \"dataDescriptor\", \"@context\", \"dataQualityFile\", \"dataSampleFile\","
                  + " \"resourceType\", \"resourceServerRegURL\",\"resourceType\","
                  + " \"location\", \"iudxResourceAPIs\", \"itemCreatedAt\",\"nsdi\","
                  + "\"icon_base64\"]},\"size\": 10000}";
  public static final String RESOURCE_ACCESSPOLICY_COUNT =
          "{\"size\": 0,\"aggs\":{\"results\":{\"terms\":{\"field\":\"resourceGroup.keyword\","
                  + "\"size\":10000},\"aggs\":{\"access_policies\":{\"terms\":{\"field\":"
                  + "\"accessPolicy.keyword\",\"size\":10000},\"aggs\":{\"accessPolicy_count\":"
                  + "{\"value_count\":{\"field\":\"accessPolicy.keyword\"}}}},\"resource_count\":"
                  + "{\"value_count\":{\"field\":\"id.keyword\"}}}}}}\n";
  public static final String GET_MLAYER_INSTANCE_ICON =
          "{\"query\":{\"match\":{\"name\":\"$1\"}},\"_source\": {\"includes\": [\"icon\"]}}";
  public static final String GET_PROVIDER_AND_RESOURCEGROUP =
          "{\"query\":{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":{\"type.keyword\":"
                  + "\"iudx:ResourceGroup\"}}]}},{\"bool\":{\"must\":[{\"match\":{\"type.keyword\":"
                  + " \"iudx:Provider\"}}]}}]}},\"_source\":{\"includes\": [\"id\",\"description\","
                  + "\"type\",\"resourceGroup\",\"accessPolicy\",\"provider\",\"itemCreatedAt\","
                  + "\"instance\",\"label\"]},\"size\":10000}";
  public static final String GET_DATASET_BY_INSTANCE =
          "{\"query\":{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":{\"type.keyword\""
                  + ":\"iudx:ResourceGroup\"}},{\"match\":{\"instance.keyword\":\"$1\"}}]}},"
                  + "{\"bool\":{\"must\":[{\"match\":{\"type.keyword\":\"iudx:Provider\"}}]}}]}},"
                  + "\"aggs\":{\"provider_count\":{\"cardinality\":{\"field\":"
                  + "\"provider.keyword\"}}},\"_source\":{\"includes\":[\"id\","
                  + "\"description\",\"type\",\"resourceGroup\","
                  + "\"accessPolicy\",\"provider\",\"itemCreatedAt\",\"instance\",\"label\"]},"
                  + "\"size\":10000}";
  public static final String GET_SORTED_MLAYER_INSTANCES =
          "{\"query\": {\"match_all\":{}},\"sort\":[{\"name\":\"asc\"}],\"_source\": "
                  + "{\"includes\": [\"name\",\"cover\",\"icon\"]},\"size\":10000}";
  public static final String GET_PROVIDER_AND_RS_ID =
          "{\"query\":{\"bool\":{\"must\":[{\"match\":{\"type.keyword\": \"iudx:ResourceGroup\"}},"
                  + "{\"match\":{\"id.keyword\":\"$1\"}}]}},\"_source\": [\"provider\",\"cos\"]}";
  public static final String INSTANCE_FILTER = "{\"match\":" + "{\"instance\": \"" + "$1" + "\"}}";
  public static final String BOOL_MUST_QUERY = "{\"query\":{\"bool\":{\"must\":[$1]}}}";
  public static final String BOOL_SHOULD_QUERY = "{\"query\":{\"bool\":{\"should\":[$1]}}}";
  public static final String SHOULD_QUERY = "{\"bool\":{\"should\":$1}}";
  public static final String MUST_QUERY = "{\"bool\":{\"must\":$1}}";
  public static final String FILTER_QUERY = "{\"bool\":{\"filter\":[$1]}}";
  public static final String MATCH_QUERY = "{\"match\":{\"$1\":\"$2\"}}";
  public static final String TERM_QUERY = "{\"term\":{\"$1\":\"$2\"}}";
  public static final String GET_RATING_DOCS =
          "{\"query\": {\"bool\": {\"must\": [ { \"match\": {\"$1\":\"$2\" } }, "
                  + "{ \"match\": { \"status\": \"approved\" } } ] } } , "
                  + "\"_source\": [\"rating\",\"id\"] }";
  public static final String GET_AVG_RATING_PREFIX =
          "{\"aggs\":{\"results\":{\"terms\":{\"field\":\"id.keyword\"},"
                  + "\"aggs\":{\"average_rating\":{\"avg\":{\"field\":\"rating\"}}}}},"
                  + "\"query\":{\"bool\":{\"should\":[";
  public static final String GET_AVG_RATING_MATCH_QUERY = "{\"match\":{\"id.keyword\":\"$1\"}},";
  public static final String GET_AVG_RATING_SUFFIX =
          "],\"minimum_should_match\":1,\"must\":[{\"match\":{\"status\":\"approved\"}}]}}}";
  public static final String QUERY_RESOURCE_GRP =
          "{\"query\":{\"bool\":{\"should\":[{\"term\":{\"id.keyword\":\"$1\"}},{\"term\":"
                  + "{\"resourceGroup.keyword\": \"$1\"}},{\"term\":{\"provider.keyword\":\"$1\"}},"
                  + "{\"term\":{\"resourceServer.keyword\": \"$1\"}},{\"term\":"
                  + "{\"cos.keyword\": \"$1\"}}]}}}";
  public static final String NLP_SEARCH =
          "{\"query\": {\"script_score\": {\"query\": "
                  + "{\"match_all\": {}},\"script\":\"source\": \"cosineSimilarity(params.query_vector,"
                  + " '_word_vector'') + 1.0\",\"lang\":\"painless\",\"params\": \"query_vector\":"
                  + " \"$1\"}}}}}";
  public static final String NLP_LOCATION_SEARCH =
          "{\"query\": {\"script_score\": {\"query\":" + " {\"bool\": {\"should\": [";
  public static final String GET_TYPE_SEARCH =
          "{\"query\": {\"bool\": {\"filter\": [{\"terms\": "
                  + "{\"id.keyword\": [\"$1\"],\"boost\": 1}}]}},"
                  + "\"_source\": [\"cos\",\"resourceServer\",\"type\","
                  + "\"provider\",\"resourceGroup\",\"id\"]}";
  public static final String GET_RSGROUP =
          "{\"query\": {\"bool\": {\"must\": [{\"match\": "
                  + "{\"resourceServer.keyword\": \"$1\"}},"
                  + "{\"term\": {\"type.keyword\": \"iudx:Provider\"}}]}},"
                  + "\"_source\": [\"id\"],\"size\": \"10000\"}";
  public static final String GET_RS1 = "{\"query\": {\"bool\": {\"should\": [";
  public static final String GET_RS2 = "{\"match\": {\"provider.keyword\": \"$1\"}},";
  public static final String GET_RS3 = "],\"minimum_should_match\": 1}}}";
  public static final String GET_DOC_QUERY_WITH_TYPE =
          "{\"_source\":[\"$2\"],\"query\":{\"bool\": {\"must\": "
                  + "[{\"term\": {\"id.keyword\": \"$1\"}},"
                  + "{\"match\":{ \"type.keyword\": \"$3\"}}]}}}";
  /* General purpose */
  public static final String SEARCH = "search";
  public static final String COUNT = "count";
  public static final String ATTRIBUTE = "attrs";
  public static final String RESULT = "results";
  public static final String SIZE_KEY = "size";
  public static final int STATIC_DELAY_TIME = 3000;
  public static final String FILTER_PATH = "?filter_path=took,hits.total.value,hits.hits._source";
  public static final String FILTER_PATH_AGGREGATION =
          "?filter_path=hits.total.value,aggregations.results.buckets";
  public static final String FILTER_ID_ONLY_PATH =
          "?filter_path=hits.total.value,hits.hits._id&size=10000";
  public static final String FILTER_PATH_ID_AND_SOURCE =
          "?filter_path=took,hits.total.value,hits.hits._source,hits.hits._id";
  public static final String TYPE_KEY = "type";
  public static final String ID_KEYWORD = "id.keyword";
  public static final String DOC_COUNT = "doc_count";
  public static final String SUMMARY_KEY = "_summary";
  public static final String GEOSUMMARY_KEY = "_geosummary";
  /* Geo-Spatial */
  public static final String COORDINATES_KEY = "coordinates";
  public static final String GEO_BBOX = "envelope";
  public static final String GEO_CIRCLE = "Circle";
  /* Replace above source list with commented one to include comment in response for rating API */
  //    "\"_source\": [\"rating\",\"comment\",\"id\"] }";
  public static final String GEO_KEY = ".geometry";
  /* Error */
  public static final String DATABASE_BAD_QUERY = "Query Failed with status != 20x";
  public static final String NO_SEARCH_TYPE_FOUND = "No searchType found";
  public static final String ERROR_DB_REQUEST = "DB request has failed";
  public static final String INSTANCE_NOT_EXISTS = "instance doesn't exist";
  static final String DESCRIPTION = "detail";
  static final String HTTP = "http";
  public static final String SHAPE_KEY = "shape";
  /* Database */
  public static final String AGGREGATION_KEY = "aggs";
  static final String FILTER_RATING_AGGREGATION = "?filter_path=hits.total.value,aggregations";
  static final String DISTANCE_IN_METERS = "m";
  static final String GEO_RELATION_KEY = "relation";
  public static final String GEO_SHAPE_KEY = "geo_shape";
  static final String EMPTY_RESPONSE = "Empty response";
  static final String COUNT_UNSUPPORTED = "Count is not supported with filtering";
  static final String INVALID_SEARCH = "Invalid search request";
  static final String DOC_EXISTS = "item already exists";

  /////////////////////////////

  /** General. */

  public static final String CONFIG_FILE = "config.properties";
  public static final String OPTIONAL_MODULES = "optionalModules";
  public static final String IS_SSL = "ssl";
  public static final String PORT = "httpPort";
  public static final String KEYSTORE_PATH = "keystorePath";
  public static final String KEYSTORE_PASSWORD = "keystorePassword";
  public static final String DATABASE_IP = "databaseIP";
  public static final String DATABASE_PORT = "databasePort";
  public static final String DATABASE_UNAME = "databaseUser";
  public static final String DOC_INDEX = "docIndex";
  public static final String RATING_INDEX = "ratingIndex";
  public static final String MLAYER_INSTANCE_INDEX = "mlayerInstanceIndex";
  public static final String MLAYER_DOMAIN_INDEX = "mlayerDomainIndex";

  public static final String PUBLIC_KEY = "publicKey";
  public static final String DATABASE_PASSWD = "databasePassword";
  public static final String SOURCE = "_source";

  public static final Pattern UUID_PATTERN =
          Pattern.compile(
                  "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$");




  /** Item type. */
  public static final String RELATIONSHIP = "relationship";
  public static final String RESOURCE = "resource";
  public static final String RESOURCE_GRP = "resourceGroup";
  public static final String RESOURCE_SVR = "resourceServer";
  public static final String PROVIDER = "provider";
  public static final String PROVIDERS = "providers";
  public static final String ALL = "all";
  public static final String COS = "cos";
  public static final String OWNER = "owner";
  public static final String COS_ADMIN = "cos_admin";
  public static final String PROVIDER_USER_ID = "ownerUserId";
  public static final String RESOURCE_SERVER_URL = "resourceServerRegURL";
  public static final String COS_ITEM = "cos";
  public static final String RESOURCETYPE = "resourceType";
  public static final String MALFORMED_ID = "Malformed Id ";



  /** Item types. */
  public static final String ITEM_TYPE_RESOURCE = "Resource";
  public static final String ITEM_TYPE_RESOURCE_GROUP = "ResourceGroup";
  public static final String ITEM_TYPE_RESOURCE_SERVER = "ResourceServer";
  public static final String ITEM_TYPE_PROVIDER = "Provider";
  public static final String ITEM_TYPE_COS = "COS";
  public static final String ITEM_TYPE_OWNER = "Owner";
  public static final String ITEM_TYPE_INSTANCE = "Instance";
  public static final String TEMPORAL_SEARCH_REGEX = "(.*)temporalSearch(.*)";

  public static final ArrayList<String> ITEM_TYPES =
          new ArrayList<String>(Arrays.asList(ITEM_TYPE_RESOURCE, ITEM_TYPE_RESOURCE_GROUP,
                  ITEM_TYPE_RESOURCE_SERVER, ITEM_TYPE_PROVIDER, ITEM_TYPE_COS, ITEM_TYPE_OWNER));

  public static final String AGGREGATIONS = "aggregations";
  public static final String INSTANCE = "instance";
  public static final String BUCKETS = "buckets";
  public static final String ITEM = "item";

  public static final String RESOURCE_ID = "resourceId";

  public static final String ITEM_TYPE = "itemType";

  public static final String PROPERTY = "property";

  /** GeoRels. */
  public static final String GEOREL_WITHIN = "within";
  public static final String GEOREL_NEAR = "near";
  public static final String GEOREL_COVERED_BY = "coveredBy";
  public static final String GEOREL_INTERSECTS = "intersects";
  public static final String GEOREL_EQUALS = "equals";
  public static final String GEOREL_DISJOINT = "disjoint";

  /** Geometries. */

  public static final String GEORELATION = "georel";
  public static final String INTERSECTS = "intersects";
  public static final String LOCATION = "location";
  public static final String MAX_DISTANCE = "maxDistance";
  public static final String POINT = "Point";
  public static final String COORDINATES = "coordinates";
  public static final String Q_VALUE = "q";
  public static final String LIMIT = "limit";
  public static final String OFFSET = "offset";

  /** SearchTypes. */
  public static final String SEARCH_TYPE_GEO = "geoSearch_";
  public static final String SEARCH_TYPE_TEXT = "textSearch_";
  public static final String SEARCH_TYPE_ATTRIBUTE = "attributeSearch_";
  public static final String SEARCH_TYPE_TAGS = "tagsSearch_";
  public static final String RESPONSE_FILTER = "responseFilter_";

  public static final String MESSAGE = "detail";
  public static final String RESULTS = "results";
  public static final String METHOD = "method";
  public static final String HTTP_METHOD = "httpMethod";
  public static final String STATUS = "title";
  public static final String TITLE = "title";
  public static final String TYPE = "type";
  public static final String DETAIL = "detail";
  public static final String FAILED = "failed";
  public static final String ERROR = "error";
  public static final String DESC = "detail";

  /** DB Query. */
  public static final String TOTAL_HITS = "totalHits";
  public static final String INCLUDE_FIELDS = "includeFields";
  public static final String FIELD = "field";
  public static final String QUERY_KEY = "query";
  public static final String HITS = "hits";
  public static final String TOTAL = "total";
  public static final String TERM = "term";
  public static final String NAME = "name";
  public static final String FILTER = "filter";
  public static final String TAGS = "tags";

  public static final String AVERAGE_RATING = "average_rating";
  public static final String TOTAL_RATINGS = "totalRatings";
  public static final String ICON_BASE64 = "icon_base64";
  public static final String PROVIDER_DES = "providerDescription";
  public static final String RESOURCE_COUNT = "resourceCount";
  public static final String PROVIDER_COUNT = "providerCount";
  public static final String RESOURCE_GROUP_COUNT = "resourceGroupCount";

  /** HTTP Methods. */
  public static final String REQUEST_GET = "GET";
  public static final String REQUEST_POST = "POST";
  public static final String REQUEST_PUT = "PUT";
  public static final String REQUEST_PATCH = "PATCH";
  public static final String REQUEST_DELETE = "DELETE";

  /** Error Messages. */
  public static final String DATABASE_ERROR = "DB Error. Check logs for more information";

  /** Operation type. */
  public static final String INSERT = "insert";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";

  /** Limits/Constraints. */
  public static final long COORDINATES_SIZE = 10;
  public static final int COORDINATES_PRECISION = 6;
  public static final int STRING_SIZE = 100;
  public static final int PROPERTY_SIZE = 4;
  public static final int VALUE_SIZE = 4;
  public static final int FILTER_VALUE_SIZE = 10;
  public static final int ID_SIZE = 512;
  public static final int INSTANCE_SIZE = 100;
  public static final int FILTER_PAGINATION_SIZE = 10000;
  public static final int OFFSET_PAGINATION_SIZE = 9999;
  public static final int MAX_RESULT_WINDOW = 10000;
  public static final int MAXDISTANCE_LIMIT = 10000; // 10KM
  public static final int SERVICE_TIMEOUT = 3000;

  public static final int POPULAR_DATASET_COUNT = 6;
  public static final String FILTER_PAGINATION_FROM = "0";
  public static final String MAX_LIMIT = "10000";


  public static final String SUCCESS = "Success";

  /* URN Codes */
  public static final String TYPE_WRONG_PROVIDER = "urn:dx:cat:WrongProvider";
  public static final String TYPE_WRONG_RESOURCESERVER = "urn:dx:cat:WrongResourceServer";
  public static final String TYPE_WRONG_RESOURCEGROUP = "urn:dx:cat:WrongResourceGroup";
  public static final String TYPE_INVALID_SCHEMA = "urn:dx:cat:InvalidSchema";
  public static final String TYPE_ID_NONEXISTANT = "urn:dx:cat:IdNonExistant";
  public static final String TYPE_ALREADY_EXISTS = "urn:dx:cat:AlreadyExists";
  public static final String TYPE_SUCCESS = "urn:dx:cat:Success";
  public static final String TYPE_FAIL = "urn:dx:cat:Fail";
  public static final String TYPE_ACCESS_DENIED = "urn:dx:cat:AccessDenied";
  public static final String TYPE_TOKEN_INVALID = "urn:dx:cat:InvalidAuthorizationToken";
  public static final String TYPE_MISSING_TOKEN = "urn:dx:cat:MissingAuthorizationToken";
  public static final String TYPE_ITEM_NOT_FOUND = "urn:dx:cat:ItemNotFound";
  public static final String TYPE_INVALID_SYNTAX = "urn:dx:cat:InvalidSyntax";
  public static final String TYPE_MISSING_PARAMS = "urn:dx:cat:MissingParams";
  public static final String TYPE_INTERNAL_SERVER_ERROR = "urn:dx:cat:InternalError";
  public static final String TYPE_OPERATION_NOT_ALLOWED = "urn:dx:cat:OperationNotAllowed";
  public static final String TYPE_LINK_VALIDATION_FAILED = "urn:dx:cat:LinkValidationFailed";
  public static final String TYPE_DB_ERROR = "urn:dx:cat:DatabaseError";
  public static final String TYPE_CONFLICT = "urn:dx:cat:Conflicts";

  public static final String TITLE_WRONG_PROVIDER = "Wrong Provider";
  public static final String TITLE_WRONG_RESOURCESERVER = "Wrong Resource Server";
  public static final String TITLE_WRONG_RESOURCEGROUP = "Wrong Resource Group";
  public static final String TITLE_INVALID_SCHEMA = "Invalid Schema";
  public static final String TITLE_ID_NONEXISTANT = "ID doesn't exist";
  public static final String TITLE_ALREADY_EXISTS = "Item already exists";
  public static final String TITLE_SUCCESS = "Success";
  public static final String TITLE_TOKEN_INVALID = "Token is invalid";
  public static final String TITLE_MISSING_TOKEN = "Token is missing";
  public static final String TITLE_ITEM_NOT_FOUND = "Item is not found";
  public static final String TITLE_INVALID_SYNTAX = "Invalid Syntax";
  public static final String TITLE_MISSING_PARAMS = "Missing parameters";

  public static final String TITLE_INTERNAL_SERVER_ERROR = "Internal error";
  public static final String TITLE_OPERATION_NOT_ALLOWED = "Operation not allowed";
  public static final String TITLE_LINK_VALIDATION_FAILED = "Link Validation Failed";
  public static final String TITLE_REQUIREMENTS_NOT_MET =
          "Resource usage requirements not satisfied";

  public static final String DETAIL_CONFLICT = "Conflicts";
  public static final String DETAIL_INTERNAL_SERVER_ERROR = "Internal error";
  public static final String DETAIL_WRONG_ITEM_TYPE = "Wrong Item Type";
  public static final String DETAIL_ID_NOT_FOUND = "id not present in the request";
  public static final String DETAIL_ITEM_NOT_FOUND = "Item not found";

  public static final String TYPE_INVALID_GEO_PARAM = "urn:dx:cat:InvalidGeoParam";
  public static final String TITLE_INVALID_GEO_PARAM = "Geoquery parameter error";

  public static final String TYPE_INVALID_GEO_VALUE = "urn:dx:cat:InvalidGeoValue";
  public static final String TITLE_INVALID_GEO_VALUE = "Geoquery value error";
  public static final String TITLE_INVALID_UUID = "Invalid syntax of uuid";



  public static final String DETAIL_INVALID_COORDINATE_POLYGON = "Coordinate mismatch (Polygon)";
  public static final String DETAIL_INVALID_BBOX = "Issue with bbox coordinates";
  public static final String DETAIL_INVALID_GEO_PARAMETER = "Missing/Invalid geo parameters";
  public static final String DETAIL_INVALID_RESPONSE_FILTER =
          "Missing/Invalid responseFilter parameters";

  public static final String DETAIL_INVALID_TOKEN = "Authorization failed, Invalid token.";

  public static final String TYPE_INVALID_PROPERTY_PARAM = "urn:dx:cat:InvalidProperty";
  public static final String TITLE_INVALID_PROPERTY_PARAM = "Invalid Property";
  public static final String TYPE_INVALID_PROPERTY_VALUE = "urn:dx:cat:InvalidPropertyValue";
  public static final String TITLE_INVALID_PROPERTY_VALUE = "Invalid Property Values";

  public static final String TYPE_INVALID_QUERY_PARAM_VALUE = "urn:dx:cat:InvalidParamValue";
  public static final String TYPE_INVALID_UUID = "urn:dx:cat:InvalidUUID";

  public static final String TITLE_INVALID_QUERY_PARAM_VALUE = "Invalid value for a query param";
  public static final String TYPE_BAD_TEXT_QUERY = "urn:dx:cat:BadTextQuery";
  public static final String TITLE_BAD_TEXT_QUERY = "Bad text query values";

  public static final String TYPE_BAD_FILTER = "urn:dx:cat:BadFilter";
  public static final String TITLE_BAD_FILTER = "Bad filters applied";
  public static final String INSTANCE_CREATION_SUCCESS = "Instance created successfully.";
  public static final String WRONG_INSTANCE_NAME = "Requested Body Instance Name wrong";
  public static final String TITLE_WRONG_INSTANCE_NAME = "Inavlid Requested Body";
  public static final String TITLE_DB_ERROR = "database error";
  public static final String TYPE_INVALID_SEARCH_ERROR = "urn:dx:cat:InvalidRelationSearch";
  public static final String TITLE_INVALID_SEARCH_ERROR = "Invalid relationship type search";
  public static final String STAC_CREATION_SUCCESS = "Stac created successfully.";
  public static final String STAC_DELETION_SUCCESS = "Stac deleted successfully.";
  public static final String DETAIL_INVALID_SCHEMA = "Invalid schema provided";
  public static final String NO_CONTENT_AVAILABLE = "No Content Available";


}
