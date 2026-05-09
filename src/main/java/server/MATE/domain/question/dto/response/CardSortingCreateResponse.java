package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.CardSorting;

import java.util.List;

public record CardSortingCreateResponse(
        Long questionId,
        Long cardSortingId,
        List<String> cards,
        List<CardSortingCategoryResponse> categories
) {
    public static CardSortingCreateResponse from(CardSorting entity) {
        return new CardSortingCreateResponse(
                entity.getQuestion().getId(),
                entity.getId(),
                entity.getCards(),
                entity.getCategories().stream()
                        .map(CardSortingCategoryResponse::new)
                        .toList()
        );
    }
}
