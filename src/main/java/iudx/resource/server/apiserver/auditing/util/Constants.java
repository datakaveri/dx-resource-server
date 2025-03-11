package iudx.resource.server.apiserver.auditing.util;

public class Constants {

  public static final String ID = "id";
  /* Temporal */
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endTime";
  public static final String BETWEEN = "between";
  public static final String TIME_RELATION = "timeRelation";
  public static final String DURING = "during";
  public static final String HEADER_OPTIONS = "options";

  public static final String ORIGIN = "origin";
  public static final String ORIGIN_SERVER = "rs-server";
  public static final String CONSENT_LOG = "consent-log";
  public static final String PRIMARY_KEY = "primaryKey";
  public static final String AUDITING_EXCHANGE = "test-audit";//"auditing";
  public static final String ROUTING_KEY = "#####";//"#";
  public static final String TOTAL = "total";
  public static final String TABLE_NAME = "tableName";

  /* configs */
  public static final String DATABASE_IP = "meteringDatabaseIP";
  public static final String DATABASE_PORT = "meteringDatabasePort";
  public static final String DATABASE_NAME = "meteringDatabaseName";
  public static final String DATABASE_USERNAME = "meteringDatabaseUserName";
  public static final String DATABASE_PASSWORD = "meteringDatabasePassword";
  public static final String DATABASE_TABLE_NAME = "meteringDatabaseTableName";
  public static final String POOL_SIZE = "meteringPoolSize";

  /* Errors */
  public static final String SUCCESS = "successful operations";
  public static final String DETAIL = "detail";
  public static final String TITLE = "title";
  public static final String RESULTS = "results";
  /* Database */
  public static final String ERROR = "Error";
  public static final String QUERY_KEY = "query";
  public static final String TYPE_KEY = "type";
  public static final String ERROR_TYPE = "type";
  public static final String PROVIDER_ID = "providerID";
  public static final String CONSUMER_ID = "consumerID";
  public static final String ENDPOINT = "endPoint";
  public static final String IID = "iid";
  public static final String RESOURCE_ID = "resourceId";
  public static final String RESPONSE_SIZE = "response_size";

  /* Metering Service Constants*/
  public static final String TIME_RELATION_NOT_FOUND = "Time relation not found.";
  public static final String TIME_NOT_FOUND = "Time interval not found.";
  public static final String USERID_NOT_FOUND = "User Id not found.";
  public static final String INVALID_DATE_TIME = "invalid date-time";

  public static final String API = "api";
  public static final String USER_ID = "userid";
  public static final String TOTAL_HITS = "totalHits";
  public static final String CONSUMERID_TIME_INTERVAL_COUNT_QUERY =
      "SELECT count(*) FROM $0 where time between '$1' and '$2' and userid='$3'";

  public static final String PROVIDERID_TIME_INTERVAL_COUNT_QUERY =
      "SELECT count(*) FROM $0 where time between '$1' and '$2' and providerid='$3'";

  public static final String CONSUMERID_TIME_INTERVAL_READ_QUERY =
      "SELECT * FROM $0 where time between '$1' and '$2' and userid='$3'";

  public static final String PROVIDERID_TIME_INTERVAL_READ_QUERY =
      "SELECT * FROM $0 where time between '$1' and '$2' and providerid='$3'";

  public static final String INVALID_DATE_DIFFERENCE =
      "Difference between dates cannot be less than 1 Minute.";
  public static final String ROLE = "role";

  public static final String OVERVIEW_QUERY =
      "SELECT month,year,COALESCE(counts, 0) as counts\n"
          + "FROM  (\n"
          + "   SELECT day::date ,to_char(date_trunc('month', day),'FMmonth') as month"
          + ",extract('year' from day) as year\n"
          + "   FROM   generate_series(timestamp '$0'\n"
          + "                        , timestamp '$1'\n"
          + "                        , interval  '1 month') day\n"
          + "   ) d\n"
          + "LEFT  JOIN (\n"
          + "   SELECT date_trunc('month', time)::date AS day\n"
          + "        , count(api) as counts \n"
          + "   FROM   auditing_rs\n"
          + "   WHERE  time between '$2'\n"
          + "   AND '$3'\n";

  public static final String GROUPBY =
      "\n" + "   GROUP  BY 1\n" + "   ) t USING (day)\n" + "ORDER  BY day";
  public static final String SUMMARY_QUERY_FOR_METERING =
      "select resourceid,count(*) from auditing_rs ";
  public static final String GROUPBY_RESOURCEID = " group by resourceid";
  public static final String USERID_SUMMARY = " and userid = '$9' ";
  public static final String USERID_SUMMARY_WITHOUT_TIME = " userid = '$9' ";
  public static final String PROVIDERID_SUMMARY = " and providerid = '$8' ";
  public static final String PROVIDERID_SUMMARY_WITHOUT_TIME = " providerid = '$8' ";
}
