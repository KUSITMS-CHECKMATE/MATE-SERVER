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

    @Test
    @DisplayName("카드소팅 카드명이 16자를 초과하면 검증에 실패한다")
    void failsWhenCardSortingCardLabelExceedsSixteenCharacters() {
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(
                new CardSortingCreateRequest(
                        "카드소팅",
                        "설명",
                        List.of("12345678901234567", "청바지", "운동화", "코트"),
                        List.of("상의")
                )
        ));

        Set<ConstraintViolation<QuestionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("questions[0].cards[0].<list element>");
    }

    @Test
    @DisplayName("카드소팅 카테고리명이 12자를 초과하면 검증에 실패한다")
    void failsWhenCardSortingCategoryLabelExceedsTwelveCharacters() {
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(
                new CardSortingCreateRequest(
                        "카드소팅",
                        "설명",
                        List.of("티셔츠", "청바지", "운동화", "코트"),
                        List.of("1234567890123")
                )
        ));

        Set<ConstraintViolation<QuestionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("questions[0].categories[0].<list element>");
    }
}
