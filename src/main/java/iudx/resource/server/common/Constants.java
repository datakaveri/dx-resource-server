package iudx.resource.server.common;

public class Constants {

  /** service proxy addresses * */
  public static final String PG_SERVICE_ADDRESS = "iudx.rs.pgsql.service";
  public static final String REDIS_SERVICE_ADDRESS = "iudx.rs.redis.service";

  public static final String CACHE_SERVICE_ADDRESS = "iudx.rs.cache.service";
  public static final String LATEST_SERVICE_ADDRESS = "iudx.rs.latest.service";
  public static final String AUTH_SERVICE_ADDRESS = "iudx.rs.authentication.service";
  public static final String ASYNC_SERVICE_ADDRESS = "iudx.rs.async.service";
  public static final String DATABASE_SERVICE_ADDRESS = "iudx.rs.database.service";
  public static final String BROKER_SERVICE_ADDRESS = "iudx.rs.broker.service";
  public static final String METERING_SERVICE_ADDRESS = "iudx.rs.metering.service";
  public static final String ENCRYPTION_SERVICE_ADDRESS = "iudx.rs.encryption.service";
  public static final String CREATE_INGESTION_SQL =
      "INSERT INTO "
          + "adaptors_details(exchange_name,resource_id,dataset_name,dataset_details_json,user_id,providerid) "
          + "VALUES('$1','$2','$3','$4','$5','$6') ";
  public static final String DELETE_INGESTION_SQL =
      "DELETE from adaptors_details where exchange_name='$0';";
  public static final String SELECT_INGESTION_SQL =
      "SELECT * from adaptors_details where providerid = '$0';";
  /* Broadcast exchanges and queues */
  public static String TOKEN_INVALID_EX = "invalid-sub";
  public static String TOKEN_INVALID_EX_ROUTING_KEY = "invalid-sub";
  public static String TOKEN_INVALID_Q = "rs-invalid-sub";
  public static String UNIQUE_ATTR_EX = "latest-data-unique-attributes";
  public static String UNIQUE_ATTR_EX_ROUTING_KEY = "unique-attribute";
  public static String UNIQUE_ATTR_Q = "rs-unique-attributes";
  public static String ASYNC_QUERY_Q = "rs-async-query";
  // postgres queries
  public static String SELECT_REVOKE_TOKEN_SQL = "SELECT * FROM revoked_tokens";
  public static String SELECT_UNIQUE_ATTRIBUTE = "SELECT * from unique_attributes";
}
