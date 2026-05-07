package server.MATE.domain.question.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.request.CardSortingCreateRequest;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.CardSortingRepository;

import java.util.List;

@Component
public class CardSortingQuestionCreateHandler extends AbstractQuestionCreateHandler<CardSortingCreateRequest> {

    private final CardSortingRepository cardSortingRepository;

    public CardSortingQuestionCreateHandler(CardSortingRepository cardSortingRepository) {
        super(CardSortingCreateRequest.class);
        this.cardSortingRepository = cardSortingRepository;
    }

    @Override
    public QuestionType supports() {
        return QuestionType.CARD_SORTING;
    }

    @Override
    protected void validateTyped(CardSortingCreateRequest item) {
    }

    @Override
    protected void createDetailTyped(Question question, CardSortingCreateRequest item) {
        CardSorting cardSorting = CardSorting.create(question, item.categories());
        cardSortingRepository.save(cardSorting);
    }

    @Override
    protected List<String> extractImageKeysTyped(CardSortingCreateRequest item) {
        return List.of();
    }
}
