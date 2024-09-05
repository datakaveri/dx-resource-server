package iudx.resource.server.metering.consumeddata.util;

import static iudx.resource.server.metering.util.Constants.DATA_CONSUMATION_DETAIL_QUERY;

import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import iudx.resource.server.metering.model.MeteringCountRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryBuilder {
  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

  public String getConsumedDataQuery(MeteringCountRequest meteringCountRequest) {

    StringBuilder query =
        new StringBuilder(
            DATA_CONSUMATION_DETAIL_QUERY
                .replace("$1", meteringCountRequest.getUserid())
                .replace("$2", meteringCountRequest.getResourceId())
                .replace("$3", meteringCountRequest.getAccessType())
                .replace("$4", meteringCountRequest.getStartTime())
                .replace("$5", meteringCountRequest.getEndTime()));

    return query.toString();
  }
}
