package server.MATE.domain.answer.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import server.MATE.domain.question.entity.QuestionType;

@JsonTypeName("AB_TEST")
public record AbTestAnswerCreateRequest(
        @NotNull Long questionId,
        String selected
) implements AnswerCreateItem {

    @Override
    public QuestionType type() {
        return QuestionType.AB_TEST;
    }
}
