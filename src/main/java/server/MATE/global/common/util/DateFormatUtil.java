package server.MATE.global.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateFormatUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private DateFormatUtil() {
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(FORMATTER);
    }
}
