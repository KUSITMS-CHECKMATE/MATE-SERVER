package server.MATE.domain.answer.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import server.MATE.domain.question.entity.QuestionType;

@JsonTypeName("AB_TEST")
public record AbTestAnswerCreateRequest(
        @NotNull Long questionId,
        @NotNull @Pattern(regexp = "A|B", message = "selected 값은 A 또는 B여야 합니다") String selected
) implements AnswerCreateItem {

    @Override
    public QuestionType type() {
        return QuestionType.AB_TEST;
    }
}
