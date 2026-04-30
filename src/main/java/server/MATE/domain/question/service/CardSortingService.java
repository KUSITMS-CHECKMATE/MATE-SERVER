package server.MATE.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import server.MATE.domain.question.dto.request.CardSortingCreateRequest;
import server.MATE.domain.question.dto.response.CardSortingCreateResponse;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.enums.QuestionType;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class CardSortingService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final CardSortingRepository cardSortingRepository;

    @Transactional
    public CardSortingCreateResponse createCardSorting(Long testId, CardSortingCreateRequest request) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BaseException(ErrorCode.TEST_004));

        Question question = Question.create(test, QuestionType.CARD_SORTING, request.title(), request.description(), null);
        questionRepository.save(question);

        CardSorting cardSorting = CardSorting.create(question, request.categories());
        cardSortingRepository.save(cardSorting);

        return CardSortingCreateResponse.from(cardSorting);
    }
}
