package server.MATE.domain.answer.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;

@JsonTypeName("TREE_TEST")
public record TreeTestAnswerCreateRequest(
        @NotNull Long questionId,
        @NotNull Long nodeId,
        @NotNull @NotEmpty List<Long> path
) implements AnswerCreateItem {

    @Override
    public QuestionType type() {
        return QuestionType.TREE_TEST;
    }
}
