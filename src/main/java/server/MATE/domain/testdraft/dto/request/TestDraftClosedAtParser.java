package server.MATE.domain.testdraft.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class TestDraftClosedAtParser {

    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    private TestDraftClosedAtParser() {
    }

    public static LocalDateTime parse(String value) {
        try {
            LocalDate date = LocalDate.parse(value, FORMATTER);
            return date.atTime(LocalTime.of(23, 59, 59));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid closedAt format", e);
        }
    }
}
