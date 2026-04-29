package server.MATE.domain.cardsorting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.cardsorting.dto.CardSortingCreateRequest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.testRepository;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.ErrorCode;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CardSortingService {

    private final QuestionRepository questionRepository;
    private final testRepository testRepository;

    @Transactional
    public void createCardSorting(CardSortingCreateRequest request) {
        Long testId = Objects.requireNonNull(request.testId());
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BaseException(ErrorCode.TEST_001));

        Question cardSortingQuestion = Question.createCardSortingQuestion(
                test,
                request.title(),
                request.description(),
                request.sequence(),
                request.categories()
        );
        questionRepository.save(Objects.requireNonNull(cardSortingQuestion));
    }
}
