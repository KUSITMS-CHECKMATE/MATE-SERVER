package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.entity.CardSorting;

import java.util.List;

public record CardSortingCreateResponse(
        Long questionId,
        Long cardSortingId,
        List<String> cards
) {
    public static CardSortingCreateResponse from(CardSorting cardSorting) {
        return new CardSortingCreateResponse(
                cardSorting.getQuestion().getId(),
                cardSorting.getId(),
                cardSorting.getCards()
        );
    }
}
