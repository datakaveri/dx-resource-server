package iudx.resource.server.metering.util;

public class Constants {

  public static final String ID = "id";
  public static final String TIME = "time";
  public static final String EXCHANGE_NAME = "auditing";
  public static final String ROUTING_KEY = "#";
  /* Temporal */
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endTime";
  public static final String TIME_RELATION = "timeRelation";
  public static final String DURING = "during";
  public static final String BETWEEN = "between";

  /* Errors */
  public static final String SUCCESS = "Success";
  public static final String FAILED = "Failed";
  public static final String DETAIL = "detail";
  public static final String TITLE = "title";
  public static final String RESULTS = "results";
  public static final String STATUS = "status";
  public static final String TABLE_NAME = "tableName";

  /* Database */
  public static final String ERROR = "Error";
  public static final String QUERY_KEY = "query";
  public static final String TOTAL = "total";
  public static final String RESPONSE_ARRAY = "responseArray";
  public static final String TOTAL_HITS = "totalHits";
  public static final String TYPE_KEY = "type";
  public static final String PROVIDER_ID = "providerID";
  public static final String CONSUMER_ID = "consumerID";
  public static final String ENDPOINT = "endPoint";
  public static final String IID = "iid";
  public static final String RESOURCE_ID = "resourceId";
  public static final String PRIMARY_KEY = "primaryKey";
  public static final String EPOCH_TIME = "epochTime";
  public static final String ISO_TIME = "isoTime";
  public static final String ORIGIN = "origin";
  public static final String ORIGIN_SERVER_SUBSCRIPTION = "rs-server-subscriptions";
  public static final String ORIGIN_SERVER = "rs-server";
  public static final String RS_DATABASE_TABLE_NAME = "auditing_rs";
  public static final String DELEGATOR_ID = "delegatorId";

  /* Metering Service Constants*/
  public static final String TIME_RELATION_NOT_FOUND = "Time relation not found.";
  public static final String TIME_NOT_FOUND = "Time interval not found.";
  public static final String USERID_NOT_FOUND = "User Id not found.";
  public static final String INVALID_DATE_TIME = "invalid date-time";
  public static final String INVALID_PROVIDER_REQUIRED = "provider id required.";
  public static final String INVALID_DATE_DIFFERENCE =
      "Difference between dates cannot be less than 1 Minute.";

  public static final String CONSUMERID_TIME_INTERVAL_COUNT_QUERY =
      "SELECT count(*) FROM $0 where time between '$1' and '$2' and userid='$3'";

  public static final String PROVIDERID_TIME_INTERVAL_COUNT_QUERY =
      "SELECT count(*) FROM $0 where time between '$1' and '$2' and providerid='$3'";

  public static final String CONSUMERID_TIME_INTERVAL_READ_QUERY =
      "SELECT * FROM $0 where time between '$1' and '$2' and userid='$3'";

  public static final String PROVIDERID_TIME_INTERVAL_READ_QUERY =
      "SELECT * FROM $0 where time between '$1' and '$2' and providerid='$3'";

  public static final String ORDER_BY = " ORDER BY time";

  public static final String OFFSET_QUERY = " offset $8";
  public static final String LIMIT_QUERY = " limit $7";
  public static final String COUNT = "count";
  public static final String API_QUERY = " and api='$4' ";
  public static final String RESOURCEID_QUERY = " and resourceid = '$5' ";
  public static final String USER_ID_QUERY = " and userid='$6' ";

  public static final String ID_QUERY = " id>'$7' and";
  public static final String API = "api";
  public static final String USER_ID = "userid";
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

  public static String DATA_CONSUMATION_DETAIL_QUERY =
      "SELECT COALESCE(api_count, 0) AS api_count,COALESCE(consumed_data, 0) AS consumed_data FROM (SELECT COUNT(*) "
          + "AS api_count,SUM(size) AS consumed_data FROM auditing_rs "
          + "WHERE userid = '$1' AND resourceid = '$2' AND access_type = '$3' AND time BETWEEN '$4' AND '$5')";
}
