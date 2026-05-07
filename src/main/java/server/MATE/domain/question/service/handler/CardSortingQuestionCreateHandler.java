package server.MATE.domain.question.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.request.CardSortingCreateRequest;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

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
        // 카드 목록은 최소 4개, 최대 12개까지 허용함
        if (item.cards() == null || item.cards().size() < 4 || item.cards().size() > 12) {
            throw new BaseException(BaseErrorCode.COMMON_002);
        }

        // 카테고리 목록은 최소 1개, 최대 3개까지 허용함
        if (item.categories() == null || item.categories().isEmpty() || item.categories().size() > 3) {
            throw new BaseException(BaseErrorCode.COMMON_002);
        }
    }

    @Override
    protected void createDetailTyped(Question question, CardSortingCreateRequest item) {
        List<CardSorting.CategoryItem> categories = item.categories().stream()
                .map(category -> new CardSorting.CategoryItem(category.name()))
                .toList();
        CardSorting cardSorting = CardSorting.create(question, item.cards(), categories);
        cardSortingRepository.save(cardSorting);
    }

    @Override
    protected List<String> extractImageKeysTyped(CardSortingCreateRequest item) {
        return List.of();
    }
}
