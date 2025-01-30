package iudx.resource.server.metering.util;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.metering.util.Constants.*;

import io.vertx.core.json.JsonObject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DateValidation {
  private static final Logger LOGGER = LogManager.getLogger(DateValidation.class);

  public Time dateParamCheck(String start, String end) {

    // since + is treated as space in uri
    String startTime = start.trim().replaceAll("\\s", "+");
    String endTime = end.trim().replaceAll("\\s", "+");

    ZonedDateTime zdt;
    try {
      zdt = ZonedDateTime.parse(startTime);
      LOGGER.debug("Parsed time: " + zdt.toString());
      zdt = ZonedDateTime.parse(endTime);
      LOGGER.debug("Parsed time: " + zdt.toString());
    } catch (DateTimeParseException e) {
      LOGGER.error("Invalid Date exception: " + e.getMessage());
//      return new JsonObject().put(ERROR, INVALID_DATE_TIME);
      throw new DxRuntimeException(HttpStatusCode.BAD_REQUEST.getValue(), ResponseUrn.BAD_REQUEST_URN,INVALID_DATE_TIME);

    }
    ZonedDateTime startZdt = ZonedDateTime.parse(startTime);
    ZonedDateTime endZdt = ZonedDateTime.parse(endTime);

    long zonedDateTimeDayDifference = zonedDateTimeDayDifference(startZdt, endZdt);
    long zonedDateTimeMinuteDifference = zonedDateTimeMinuteDifference(startZdt, endZdt);

    LOGGER.trace(
        "PERIOD between given time day :{} , minutes :{}",
        zonedDateTimeDayDifference,
        zonedDateTimeMinuteDifference);

    if (zonedDateTimeDayDifference < 0 || zonedDateTimeMinuteDifference <= 0) {
      LOGGER.error(INVALID_DATE_DIFFERENCE);
//      return new JsonObject().put(ERROR, INVALID_DATE_DIFFERENCE);
      throw new DxRuntimeException(HttpStatusCode.BAD_REQUEST.getValue(), ResponseUrn.BAD_REQUEST_URN,INVALID_DATE_DIFFERENCE);
    }
    Time time = new Time();
    time.startTime = startTime;
    time.endTime = endTime;
    return time;
  }

  private long zonedDateTimeDayDifference(ZonedDateTime startTime, ZonedDateTime endTime) {
    LOGGER.info("zonedDateTimeDayDifference : {}",ChronoUnit.DAYS.between(startTime, endTime));
    return ChronoUnit.DAYS.between(startTime, endTime);
  }

  private long zonedDateTimeMinuteDifference(ZonedDateTime startTime, ZonedDateTime endTime) {
    LOGGER.info("zonedDateTimeMinuteDifference : {}",ChronoUnit.MINUTES.between(startTime, endTime));
    return ChronoUnit.MINUTES.between(startTime, endTime);
  }
  public static class Time{
    private String startTime;
    private String endTime;

    public String getStartTime() {
      return startTime;
    }

    public String getEndTime() {
      return endTime;
    }
  }
}
