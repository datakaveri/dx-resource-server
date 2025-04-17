package org.cdpg.dx.rs.search.util.validatorTypes;

import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static org.cdpg.dx.util.Constants.*;

public class TemporalValidator {

    private static final List<String> ALLOWED_TEMPORAL_RELATIONS = List.of(JSON_DURING, JSON_BETWEEN, JSON_BEFORE, JSON_AFTER);

    private static final int DEFAULT_TIME_LIMIT_DAYS = 10;


    private final List<String> errorMessages = new ArrayList<>();


    public boolean validateTemporalParams(JsonObject temporalParams) {
        errorMessages.clear();
        if (temporalParams == null || temporalParams.isEmpty()) {
            return true;
        }

        String timerel = temporalParams.getString(JSON_TIMEREL);
        String time = temporalParams.getString(JSON_TIME);
        String endTime = temporalParams.getString(JSON_ENDTIME);

        if (!validateTemporalRelation(timerel)) return false;
        if (!validateTimePresence(timerel, time, endTime)) return false;
        if (!validateDateFormat(time, endTime)) return false;
        if ((JSON_DURING.equalsIgnoreCase(timerel) || JSON_BETWEEN.equalsIgnoreCase(timerel))) {
            return validateTimeInterval(time, endTime);
        }
        return true;
    }

    private boolean validateTemporalRelation(String timerel) {
        if (timerel == null || timerel.isBlank()) {
            errorMessages.add("Missing temporal relation (timerel)");
            return false;
        }

        List<String> allowedRelations = ALLOWED_TEMPORAL_RELATIONS;
        if (!allowedRelations.contains(timerel.toLowerCase())) {
            errorMessages.add(String.format("Invalid timerel '%s' ", timerel));
            return false;
        }
        return true;
    }

    private boolean validateTimePresence(String timerel, String time, String endTime) {
        if (time == null || time.isBlank()) {
            errorMessages.add("Time parameter is required");
            return false;
        }

        if ((JSON_DURING.equalsIgnoreCase(timerel) || JSON_BETWEEN.equalsIgnoreCase(timerel))) {
            if (endTime == null || endTime.isBlank()) {
                errorMessages.add("Endtime parameter is required for " + timerel);
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
            errorMessages.add("Invalid date format: " + date);
            return false;
        }
    }

    private boolean validateTimeInterval(String time, String endTime) {
        try {
            ZonedDateTime start = ZonedDateTime.parse(time.replace(" ", "+"));
            ZonedDateTime end = ZonedDateTime.parse(endTime.replace(" ", "+"));

            if (end.isBefore(start)) {
                errorMessages.add("Endtime must be after time");
                return false;
            }

            Duration duration = Duration.between(start, end);
            long daysBetween = duration.toDays();
            int maxDays = DEFAULT_TIME_LIMIT_DAYS;

            if (daysBetween > maxDays) {
                errorMessages.add(String.format("Time interval exceeds %d days limit", maxDays));
                return false;
            }
            return true;
        } catch (DateTimeParseException ex) {
            errorMessages.add("Invalid date format during interval validation");
            return false;
        }
    }

}