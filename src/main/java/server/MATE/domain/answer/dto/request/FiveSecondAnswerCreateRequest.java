package server.MATE.domain.answer.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;

@JsonTypeName("FIVE_SECOND")
public record FiveSecondAnswerCreateRequest(
        @NotNull Long questionId,
        String text,
        List<Long> selectedOptionIds
) implements AnswerCreateItem {

    @Override
    public QuestionType type() {
        return QuestionType.FIVE_SECOND;
    }
}
