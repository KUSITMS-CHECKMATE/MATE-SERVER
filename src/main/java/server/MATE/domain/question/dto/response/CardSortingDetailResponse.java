package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;

public record CardSortingDetailResponse(
        Long questionId,
        Long cardSortingId,
        QuestionType type,
        Long sequence,
        String title,
        String description,
        List<String> cards,
        List<String> categories
) implements QuestionDetailItem {

    public static CardSortingDetailResponse of(Question question, CardSorting cardSorting) {
        return new CardSortingDetailResponse(
                question.getId(),
                cardSorting.getId(),
                question.getQuestionType(),
                question.getSequence(),
                question.getTitle(),
                question.getDescription(),
                List.copyOf(cardSorting.getCards()),
                List.copyOf(cardSorting.getCategories())
        );
    }
}
