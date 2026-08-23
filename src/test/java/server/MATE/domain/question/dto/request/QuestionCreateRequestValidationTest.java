package server.MATE.domain.question.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionCreateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("questions가 null이면 검증에 실패한다")
    void failsWhenQuestionsIsNull() {
        QuestionCreateRequest request = new QuestionCreateRequest(null);

        Set<ConstraintViolation<QuestionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("questions");
    }

    @Test
    @DisplayName("questions가 비어 있으면 검증에 실패한다")
    void failsWhenQuestionsIsEmpty() {
        QuestionCreateRequest request = new QuestionCreateRequest(List.of());

        Set<ConstraintViolation<QuestionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("questions");
    }

    @Test
    @DisplayName("객관식 선택지가 1개면 검증에 실패한다")
    void failsWhenObjectiveOptionsContainOnlyOneItem() {
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(
                new ObjectiveCreateRequest(
                        "객관식",
                        "설명",
                        false,
                        null,
                        null,
                        false,
                        List.of(new ObjectiveOptionRequest("A", null))
                )
        ));

        Set<ConstraintViolation<QuestionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("questions[0].options");
    }
}
