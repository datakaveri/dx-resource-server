package org.cdpg.dx.rs.search.util.validatorTypes;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.cdpg.dx.util.Constants.*;

public class TemporalValidator {

    private static final List<String> ALLOWED_TEMPORAL_RELATIONS = List.of(JSON_DURING, JSON_BETWEEN, JSON_BEFORE, JSON_AFTER);
    private static final Logger LOGGER = LogManager.getLogger(TemporalValidator.class);

    private static final int DEFAULT_TIME_LIMIT_DAYS = 10;


    public boolean validateTemporalParams(JsonObject temporalParams) {
        LOGGER.debug("Inside validateTemporalParams");
        if (temporalParams == null || temporalParams.isEmpty()) {
            return true;
        }

        String timeRel = temporalParams.getString(JSON_TIMEREL);
        String time = temporalParams.getString(JSON_TIME);
        String endTime = temporalParams.getString(JSON_ENDTIME);

        if (!validateTemporalRelation(timeRel)) return false;
        if (!validateTimePresence(timeRel, time, endTime)) return false;
        if (!validateDateFormat(time, endTime)) return false;
        if ((JSON_DURING.equalsIgnoreCase(timeRel) || JSON_BETWEEN.equalsIgnoreCase(timeRel))) {
            return validateTimeInterval(time, endTime);
        }
        return true;
    }

    private boolean validateTemporalRelation(String timerel) {
        if (timerel == null || timerel.isBlank()) {
            LOGGER.debug("Missing temporal relation (timerel)");
            return false;
        }

        if (!ALLOWED_TEMPORAL_RELATIONS.contains(timerel.toLowerCase())) {
            LOGGER.debug("Invalid timerel '{}' ", timerel);
            return false;
        }
        return true;
    }

    private boolean validateTimePresence(String timeRel, String time, String endTime) {
        if (time == null || time.isBlank()) {
            LOGGER.debug("Time parameter is required");
            return false;
        }

        if ((JSON_DURING.equalsIgnoreCase(timeRel) || JSON_BETWEEN.equalsIgnoreCase(timeRel))) {
            if (endTime == null || endTime.isBlank()) {
                LOGGER.error("Endtime parameter is required for {}", timeRel);
                return false;
            }
        }

        if(JSON_AFTER.equalsIgnoreCase(timeRel) || JSON_BEFORE.equalsIgnoreCase(timeRel)){
            if(endTime != null || time == null || time.isBlank()){
                LOGGER.error("Time rel not valid. endtime not allowed/ only time is allowed." );
                return false;
            }
        }
        return true;
    }

    private boolean validateDateFormat(String time, String endTime) {
        if (!validateSingleDate(time)) return false;
        return endTime == null || validateSingleDate(endTime);
    }

    private boolean validateSingleDate(String date) {
        try {
            String normalizedDate = date.trim().replace(" ", "+");
            ZonedDateTime.parse(normalizedDate);
            return true;
        } catch (DateTimeParseException e) {
            LOGGER.error("Invalid date format: {}",date);
            return false;
        }
    }

    private boolean validateTimeInterval(String time, String endTime) {
        try {
            ZonedDateTime start = ZonedDateTime.parse(time.replace(" ", "+"));
            ZonedDateTime end = ZonedDateTime.parse(endTime.replace(" ", "+"));

            if (end.isBefore(start)) {
                LOGGER.error("Endtime must be after time");
                return false;
            }

            Duration duration = Duration.between(start, end);
            long daysBetween = duration.toDays();
            int maxDays = DEFAULT_TIME_LIMIT_DAYS;

            if (daysBetween > maxDays) {
                LOGGER.error("Time interval exceeds {} days limit", maxDays);
                return false;
            }
            return true;
        } catch (DateTimeParseException ex) {
            LOGGER.error("Invalid date format during interval validation");
            return false;
        }
    }

}