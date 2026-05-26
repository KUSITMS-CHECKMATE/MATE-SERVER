package server.MATE.domain.testdraft.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import server.MATE.domain.testdraft.dto.request.TestDraftClosedAtParser;

import java.time.LocalDate;

public class ClosedAtFormatValidator implements ConstraintValidator<ClosedAtFormat, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        final LocalDate requestedDate;
        try {
            requestedDate = TestDraftClosedAtParser.parse(value).toLocalDate();
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (!requestedDate.isAfter(LocalDate.now())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("마감 기한은 오늘 이후 날짜여야 합니다.")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
