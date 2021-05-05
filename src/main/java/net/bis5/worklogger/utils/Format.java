package net.bis5.worklogger.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Format {

    private Format() {
        throw new InternalError("Utility class");
    }

    public static String toHHMM(int mins) {
        var hour = mins / 60;
        var min = mins % 60;
        return String.format("%d:%02d", hour, min);
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static String toHHMM(ZonedDateTime dateTime) {
        if (dateTime == null) { return null; }
        return TIME_FORMATTER.format(dateTime);
    }

    public static BigDecimal toHours(int mins) {
        return BigDecimal.valueOf(mins).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
