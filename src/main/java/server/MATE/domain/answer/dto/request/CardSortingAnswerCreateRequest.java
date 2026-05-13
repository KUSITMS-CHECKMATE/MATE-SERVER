package server.MATE.domain.answer.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;

@JsonTypeName("CARD_SORTING")
public record CardSortingAnswerCreateRequest(
        @NotNull Long questionId,
        List<GroupItem> groups
) implements AnswerCreateItem {

    public record GroupItem(String category, List<String> cardNames) {}

    @Override
    public QuestionType type() {
        return QuestionType.CARD_SORTING;
    }
}
