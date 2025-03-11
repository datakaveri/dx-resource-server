package iudx.resource.server.apiserver.auditing.util;

import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import iudx.resource.server.apiserver.auditing.model.OverviewRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.apiserver.auditing.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.LIMITPARAM;
import static iudx.resource.server.apiserver.util.Constants.OFFSETPARAM;

public class QueryBuilder {

  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);
  long today;
  StringBuilder monthQuery;

  public JsonObject buildMessageForRmq(JsonObject request) {
    if (!request.containsKey(ORIGIN)) {
      String primaryKey = UUID.randomUUID().toString().replace("-", "");
      String userId = request.getString(USER_ID);
      request.put(PRIMARY_KEY, primaryKey);
      request.put(USER_ID, userId);
      request.put(ORIGIN, ORIGIN_SERVER);
      LOGGER.trace("Info: Request " + request);
    }
    return request;
  }

  public JsonObject buildReadQueryFromPg(JsonObject request) {
    LOGGER.trace("buildReadQueryFromPg() started");
    String startTime = request.getString(START_TIME);
    String endTime = request.getString(END_TIME);
    String userId = request.getString(USER_ID);
    String providerId = request.getString(PROVIDER_ID);
    String databaseTableName = request.getString(TABLE_NAME);
    StringBuilder query;
    String api = request.getString(API);
    String resourceId = request.getString(RESOURCE_ID);
    String consumerId = request.getString(CONSUMER_ID);
    String limit = request.getString(LIMITPARAM);
    String offset = request.getString(OFFSETPARAM);

    if (providerId != null) {
      query =
          new StringBuilder(
              PROVIDERID_TIME_INTERVAL_READ_QUERY
                  .replace("$0", "auditing_rs")
                  .replace("$1", startTime)
                  .replace("$2", endTime)
                  .replace("$3", providerId));
      if (api != null) {
        query.append(" and api = '$5' ".replace("$5", api));
      }
      if (resourceId != null) {
        query.append(" and resourceid = '$6' ".replace("$6", resourceId));
      }
      if (consumerId != null) {
        query.append(" and userid='$6' ".replace("$6", userId));
      }
    } else {
      LOGGER.error("api {} resourceId {}", api, resourceId);
      query =
          new StringBuilder(
              CONSUMERID_TIME_INTERVAL_READ_QUERY
                  .replace("$0", "auditing_rs")
                  .replace("$1", startTime)
                  .replace("$2", endTime)
                  .replace("$3", userId));
      if (api != null) {
        query.append(" and api = '$5' ".replace("$5", api));
      }
      if (resourceId != null) {
        query.append(" and resourceid = '$6' ".replace("$6", resourceId));
      }
    }
    query.append(" limit $7".replace("$7", limit));
    query.append(" offset $8".replace("$8", offset));
    return new JsonObject().put(QUERY_KEY, query);
  }

  public JsonObject buildCountReadQueryFromPg(JsonObject request) {
    LOGGER.trace("buildCountReadQueryFromPg() started");
    String startTime = request.getString(START_TIME);
    String endTime = request.getString(END_TIME);
    String userId = request.getString(USER_ID);
    String providerId = request.getString(PROVIDER_ID);
    String databaseTableName = request.getString(TABLE_NAME);
    StringBuilder query;
    String api = request.getString(API);
    String resourceId = request.getString(RESOURCE_ID);
    String consumerId = request.getString(CONSUMER_ID);

    if (providerId != null) {
      query =
          new StringBuilder(
              PROVIDERID_TIME_INTERVAL_COUNT_QUERY
                  .replace("$0", "auditing_rs")
                  .replace("$1", startTime)
                  .replace("$2", endTime)
                  .replace("$3", providerId));
      if (api != null) {
        query.append(" and api = '$5' ".replace("$5", api));
      }
      if (resourceId != null) {
        query.append(" and resourceid = '$6' ".replace("$6", resourceId));
      }
      if (consumerId != null) {
        query.append(" and userid='$6' ".replace("$6", userId));
      }

    } else {
      query =
          new StringBuilder(
              CONSUMERID_TIME_INTERVAL_COUNT_QUERY
                  .replace("$0", "auditing_rs")
                  .replace("$1", startTime)
                  .replace("$2", endTime)
                  .replace("$3", userId));
      if (api != null) {
        query.append(" and api = '$5' ".replace("$5", api));
      }
      if (resourceId != null) {
        query.append(" and resourceid = '$6' ".replace("$6", resourceId));
      }
    }
    LOGGER.debug("query " + query);
    return new JsonObject().put(QUERY_KEY, query);
  }

  public String buildMonthlyOverviewQueryNew(OverviewRequest monthlyOverviewRequest) {
    ZonedDateTime utcNow = ZonedDateTime.now(ZoneId.of("UTC"));
    LocalDateTime utcTime = utcNow.toLocalDateTime();

    String timeYearBack = utcTime.minusYears(1).withDayOfMonth(1).toString();
    String startTime = monthlyOverviewRequest.getStartTime();
    String endTime = monthlyOverviewRequest.getEndTime();
    String timeSeriesToFirstDay;

    if (startTime != null) {
      timeSeriesToFirstDay = ZonedDateTime.parse(startTime).withDayOfMonth(1).toString();
    } else {
      timeSeriesToFirstDay = timeYearBack;
    }

    String endTimeValue;
    if (endTime != null) {
      endTimeValue = endTime;
    } else {
      endTimeValue = utcTime.toString();
    }

    String startTimeValue;
    if (startTime != null) {
      startTimeValue = startTime;
    } else {
      startTimeValue = timeYearBack;
    }

    // Replace placeholders in the base query string
    String monthQuery =
        OVERVIEW_QUERY
            .replace("$0", timeSeriesToFirstDay)
            .replace("$1", endTimeValue)
            .replace("$2", startTimeValue)
            .replace("$3", endTimeValue);

    // Create a StringBuilder for query modifications
    StringBuilder queryBuilder = new StringBuilder(monthQuery);

    // Add filters based on user roles
    if ("consumer".equalsIgnoreCase(monthlyOverviewRequest.getRole())) {
      queryBuilder
          .append(" and userid = '")
          .append(monthlyOverviewRequest.getUserId())
          .append("' ");
    } else if ("provider".equalsIgnoreCase(monthlyOverviewRequest.getRole())
        || "delegate".equalsIgnoreCase(monthlyOverviewRequest.getRole())) {
      queryBuilder
          .append(" and providerid = '")
          .append(monthlyOverviewRequest.getProviderId())
          .append("' ");
    }

    // Append GROUP BY clause
    queryBuilder.append(GROUPBY);

    // Log final query before returning
    String finalQuery = queryBuilder.toString();
    LOGGER.debug("Final MonthlyOverviewQuery: {}", finalQuery);
    return finalQuery;
  }

  public String buildSummaryOverview(OverviewRequest overviewRequest) {
    String startTime = overviewRequest.getStartTime();
    String endTime = overviewRequest.getEndTime();
    String role = overviewRequest.getRole();

    StringBuilder summaryQuery = new StringBuilder(SUMMARY_QUERY_FOR_METERING);
    if (startTime != null && endTime != null) {
      summaryQuery.append(
          " where time between '$2' AND '$3' ".replace("$2", startTime).replace("$3", endTime));
      if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {

        summaryQuery.append(PROVIDERID_SUMMARY.replace("$8", overviewRequest.getProviderId()));
      }
      if (role.equalsIgnoreCase("consumer")) {
        summaryQuery.append(USERID_SUMMARY.replace("$9", overviewRequest.getUserId()));
      }
    } else {
      if (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate")) {
        summaryQuery.append(" where ");
        summaryQuery.append(
            PROVIDERID_SUMMARY_WITHOUT_TIME.replace("$8", overviewRequest.getProviderId()));
      }
      if (role.equalsIgnoreCase("consumer")) {
        summaryQuery.append(" where ");
        summaryQuery.append(USERID_SUMMARY_WITHOUT_TIME.replace("$9", overviewRequest.getUserId()));
      }
    }
    summaryQuery.append(GROUPBY_RESOURCEID);
    return summaryQuery.toString();
  }
}
