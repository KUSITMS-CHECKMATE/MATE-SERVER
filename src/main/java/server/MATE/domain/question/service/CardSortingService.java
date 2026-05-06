package server.MATE.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.request.CardSortingCategoryItemRequest;
import server.MATE.domain.question.dto.request.CardSortingCreateRequest;
import server.MATE.domain.question.dto.response.CardSortingCreateResponse;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardSortingService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final CardSortingRepository cardSortingRepository;

    @Transactional
    public CardSortingCreateResponse createCardSorting(Long testId, CardSortingCreateRequest request) {
        ensureTestExists(testId);
        Question question = persistQuestion(testId, request);
        CardSorting cardSorting = CardSorting.create(
                question,
                request.cards(),
                mapCategoryItems(request.categories()));
        cardSortingRepository.save(cardSorting);
        return CardSortingCreateResponse.from(cardSorting);
    }

    private void ensureTestExists(Long testId) {
        if (!testRepository.existsById(testId)) {
            throw new BaseException(BaseErrorCode.TEST_004);
        }
    }

    private Question persistQuestion(Long testId, CardSortingCreateRequest request) {
        // TODO: sequence 로직은 merge 후 수정 예정
        Question question = Question.builder()
                .testId(testId)
                .questionType(QuestionType.CARD_SORTING)
                .title(request.title())
                .description(request.description())
                .sequence(null)
                .build();
        return questionRepository.save(question);
    }

    private static List<CardSorting.CategoryItem> mapCategoryItems(
            List<CardSortingCategoryItemRequest> categories
    ) {
        return categories.stream()
                .map(c -> new CardSorting.CategoryItem(c.name()))
                .toList();
    }
}
