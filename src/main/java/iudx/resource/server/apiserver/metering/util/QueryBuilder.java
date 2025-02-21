package iudx.resource.server.apiserver.metering.util;

import static iudx.resource.server.apiserver.ingestion.util.Constants.PROVIDER_ID;
import static iudx.resource.server.apiserver.metering.util.Constant.API;
import static iudx.resource.server.apiserver.metering.util.Constant.API_QUERY;
import static iudx.resource.server.apiserver.metering.util.Constant.CONSUMERID_TIME_INTERVAL_COUNT_QUERY;
import static iudx.resource.server.apiserver.metering.util.Constant.CONSUMER_ID;
import static iudx.resource.server.apiserver.metering.util.Constant.END_TIME;
import static iudx.resource.server.apiserver.metering.util.Constant.GROUPBY;
import static iudx.resource.server.apiserver.metering.util.Constant.GROUPBY_RESOURCEID;
import static iudx.resource.server.apiserver.metering.util.Constant.ORIGIN;
import static iudx.resource.server.apiserver.metering.util.Constant.ORIGIN_SERVER;
import static iudx.resource.server.apiserver.metering.util.Constant.OVERVIEW_QUERY;
import static iudx.resource.server.apiserver.metering.util.Constant.PRIMARY_KEY;
import static iudx.resource.server.apiserver.metering.util.Constant.PROVIDERID_SUMMARY;
import static iudx.resource.server.apiserver.metering.util.Constant.PROVIDERID_SUMMARY_WITHOUT_TIME;
import static iudx.resource.server.apiserver.metering.util.Constant.PROVIDERID_TIME_INTERVAL_COUNT_QUERY;
import static iudx.resource.server.apiserver.metering.util.Constant.RESOURCEID_QUERY;
import static iudx.resource.server.apiserver.metering.util.Constant.RESOURCE_ID;
import static iudx.resource.server.apiserver.metering.util.Constant.ROLE;
import static iudx.resource.server.apiserver.metering.util.Constant.START_TIME;
import static iudx.resource.server.apiserver.metering.util.Constant.SUMMARY_QUERY_FOR_METERING;
import static iudx.resource.server.apiserver.metering.util.Constant.TABLE_NAME;
import static iudx.resource.server.apiserver.metering.util.Constant.USERID_SUMMARY;
import static iudx.resource.server.apiserver.metering.util.Constant.USERID_SUMMARY_WITHOUT_TIME;
import static iudx.resource.server.apiserver.metering.util.Constant.USER_ID;
import static iudx.resource.server.apiserver.metering.util.Constant.USER_ID_QUERY;
import static iudx.resource.server.apiserver.util.Constants.ENDT;
import static iudx.resource.server.apiserver.util.Constants.STARTT;
import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryBuilder {

  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);
  StringBuilder monthQuery;
  long today;

  public JsonObject buildMessageForRmq(JsonObject request) {

    if (request.getString(ORIGIN) == null) {
      String primaryKey = UUID.randomUUID().toString().replace("-", "");
      request.put(PRIMARY_KEY, primaryKey);
      String userId = request.getString(USER_ID);
      request.put(USER_ID, userId);
      request.put(ORIGIN, ORIGIN_SERVER);
    }
    LOGGER.trace("Info: Request " + request);
    return request;
  }

  public String buildCountReadQueryFromPg(JsonObject request) {
    LOGGER.trace(request);
    String startTime = request.getString("time");
    String endTime = request.getString(END_TIME);
    String resourceId = request.getString(RESOURCE_ID);
    String userId = request.getString(USER_ID);
    String api = request.getString(API);
    String providerId = request.getString(PROVIDER_ID);
    String consumerId = request.getString(CONSUMER_ID);
    String databaseTableName = request.getString(TABLE_NAME);
    StringBuilder query = null;

    if (providerId != null) {
      query =
          new StringBuilder(
              PROVIDERID_TIME_INTERVAL_COUNT_QUERY
                  .replace("$0", databaseTableName)
                  .replace("$1", startTime)
                  .replace("$2", endTime)
                  .replace("$3", providerId));
      if (api != null) {
        query = query.append(API_QUERY.replace("$4", api));
      }
      if (resourceId != null) {
        query = query.append(RESOURCEID_QUERY.replace("$5", resourceId));
      }
      if (consumerId != null) {
        query = query.append(USER_ID_QUERY.replace("$6", consumerId));
      }
    } else {
      query =
          new StringBuilder(
              CONSUMERID_TIME_INTERVAL_COUNT_QUERY
                  .replace("$0", databaseTableName)
                  .replace("$1", startTime)
                  .replace("$2", endTime)
                  .replace("$3", userId));
      if (api != null) {
        query = query.append(API_QUERY.replace("$4", api));
      }
      if (resourceId != null) {
        query = query.append(RESOURCEID_QUERY.replace("$5", resourceId));
      }
    }
    return query.toString();
  }

  public String buildMonthlyOverview(JsonObject request) {
    String role = request.getString(ROLE);

    String current = ZonedDateTime.now().toString();
    LOGGER.debug("zone IST =" + ZonedDateTime.now());
    ZonedDateTime zonedDateTimeUtc = ZonedDateTime.parse(current);
    zonedDateTimeUtc = zonedDateTimeUtc.withZoneSameInstant(ZoneId.of("UTC"));
    LOGGER.debug("zonedDateTimeUTC UTC = " + zonedDateTimeUtc);
    LocalDateTime utcTime = zonedDateTimeUtc.toLocalDateTime();
    LOGGER.debug("UTCtime =" + utcTime);
    today = zonedDateTimeUtc.getDayOfMonth();
    String timeYearBack =
        utcTime
            .minusYears(1)
            .minusDays(today)
            .plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .toString();
    LOGGER.debug("Year back =" + timeYearBack);
    String startTime = request.getString(STARTT);
    String endTime = request.getString(ENDT);
    if (startTime != null && endTime != null) {
      ZonedDateTime timeSeries = ZonedDateTime.parse(startTime);
      String timeSeriesToFirstDay = String.valueOf(timeSeries.withDayOfMonth(1));
      LOGGER.debug("Time series = " + timeSeriesToFirstDay);
      if (role.equalsIgnoreCase("admin")) {
        monthQuery =
            new StringBuilder(
                OVERVIEW_QUERY
                    .concat(GROUPBY)
                    .replace("$0", timeSeriesToFirstDay)
                    .replace("$1", endTime)
                    .replace("$2", startTime)
                    .replace("$3", endTime));
      } else if (role.equalsIgnoreCase("consumer")) {
        String userId = request.getString(USER_ID);
        monthQuery =
            new StringBuilder(
                OVERVIEW_QUERY
                    .concat(" and userid = '$4' ")
                    .concat(GROUPBY)
                    .replace("$0", timeSeriesToFirstDay)
                    .replace("$1", endTime)
                    .replace("$2", startTime)
                    .replace("$3", endTime)
                    .replace("$4", userId));
      } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
        String providerId = request.getString("providerid");
        LOGGER.debug("Provider = {}", providerId);
        monthQuery =
            new StringBuilder(
                OVERVIEW_QUERY
                    .concat(" and providerid = '$4' ")
                    .concat(GROUPBY)
                    .replace("$0", timeSeriesToFirstDay)
                    .replace("$1", endTime)
                    .replace("$2", startTime)
                    .replace("$3", endTime)
                    .replace("$4", providerId));
      }
    } else {
      if (role.equalsIgnoreCase("admin")) {
        monthQuery =
            new StringBuilder(
                OVERVIEW_QUERY
                    .concat(GROUPBY)
                    .replace("$0", timeYearBack)
                    .replace("$1", utcTime.toString())
                    .replace("$2", timeYearBack)
                    .replace("$3", utcTime.toString()));
      } else if (role.equalsIgnoreCase("consumer")) {
        String userId = request.getString(USER_ID);
        monthQuery =
            new StringBuilder(
                OVERVIEW_QUERY
                    .concat(" and userid = '$4' ")
                    .concat(GROUPBY)
                    .replace("$0", timeYearBack)
                    .replace("$1", utcTime.toString())
                    .replace("$2", timeYearBack)
                    .replace("$3", utcTime.toString())
                    .replace("$4", userId));
      } else if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
        String providerId = request.getString("providerid");
        LOGGER.debug("Provider = {}", providerId);
        monthQuery =
            new StringBuilder(
                OVERVIEW_QUERY
                    .concat(" and providerid = '$4' ")
                    .concat(GROUPBY)
                    .replace("$0", timeYearBack)
                    .replace("$1", utcTime.toString())
                    .replace("$2", timeYearBack)
                    .replace("$3", utcTime.toString())
                    .replace("$4", providerId));
      }
    }

    return monthQuery.toString();
  }

  public String buildSummaryOverview(JsonObject request) {
    String startTime = request.getString(STARTT);
    String endTime = request.getString(ENDT);
    String role = request.getString(ROLE);

    StringBuilder summaryQuery = new StringBuilder(SUMMARY_QUERY_FOR_METERING);
    if (startTime != null && endTime != null) {
      summaryQuery.append(
          " where time between '$2' AND '$3' ".replace("$2", startTime).replace("$3", endTime));
      if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
        String providerId = request.getString("providerid");
        LOGGER.debug("Provider = {}", providerId);
        summaryQuery.append(PROVIDERID_SUMMARY.replace("$8", providerId));
      }
      if (role.equalsIgnoreCase("consumer")) {
        String userid = request.getString(USER_ID);
        summaryQuery.append(USERID_SUMMARY.replace("$9", userid));
      }
    } else {
      if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
        String providerId = request.getString("providerid");
        LOGGER.debug("Provider = {}", providerId);
        summaryQuery.append(" where ");
        summaryQuery.append(PROVIDERID_SUMMARY_WITHOUT_TIME.replace("$8", providerId));
      }
      if (role.equalsIgnoreCase("consumer")) {
        String userid = request.getString(USER_ID);
        summaryQuery.append(" where ");
        summaryQuery.append(USERID_SUMMARY_WITHOUT_TIME.replace("$9", userid));
      }
    }
    summaryQuery.append(GROUPBY_RESOURCEID);
    return summaryQuery.toString();
  }
}
