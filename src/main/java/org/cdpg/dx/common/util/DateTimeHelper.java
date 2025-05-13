package org.cdpg.dx.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DateTimeHelper {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Returns current time as String using default pattern
    public static String getCurrentTimeString() {
        return LocalDateTime.now().format(DEFAULT_FORMATTER);
    }

    // Returns current time as String with a custom pattern
    public static String getCurrentTimeString(String pattern) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    // Parse string to LocalDateTime
    public static Optional<LocalDateTime> parse(String dateTimeStr) {
        try {
            return Optional.of(LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Format LocalDateTime to String
    public static String format(LocalDateTime dateTime) {
        return dateTime.format(DEFAULT_FORMATTER);
    }
}

