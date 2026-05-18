package server.MATE.domain.answer.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;

@JsonTypeName("OBJECTIVE")
public record ObjectiveAnswerCreateRequest(
        @NotNull Long questionId,
        @NotNull List<Long> optionIds,
        String otherText
) implements AnswerCreateItem {

    @Override
    public QuestionType type() {
        return QuestionType.OBJECTIVE;
    }
}
