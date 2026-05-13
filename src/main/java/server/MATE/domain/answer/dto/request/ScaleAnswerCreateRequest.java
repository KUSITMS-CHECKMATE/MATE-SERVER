package server.MATE.domain.answer.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import server.MATE.domain.question.entity.QuestionType;

@JsonTypeName("SCALE")
public record ScaleAnswerCreateRequest(
        @NotNull Long questionId,
        Integer value
) implements AnswerCreateItem {

    @Override
    public QuestionType type() {
        return QuestionType.SCALE;
    }
}
