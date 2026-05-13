package server.MATE.domain.answer.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import server.MATE.domain.question.entity.QuestionType;

@JsonTypeName("TREE_TEST")
public record TreeTestAnswerCreateRequest(
        @NotNull Long questionId,
        Long nodeId
) implements AnswerCreateItem {

    @Override
    public QuestionType type() {
        return QuestionType.TREE_TEST;
    }
}
