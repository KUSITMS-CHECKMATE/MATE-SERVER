package server.MATE.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import server.MATE.domain.question.dto.request.CardSortingCreateRequest;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class CardSortingService {

    private final QuestionRepository questionRepository;
    private final CardSortingRepository cardSortingRepository;

    @Transactional
    public void createCardSorting(Long questionId, CardSortingCreateRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BaseException(ErrorCode.QUESTION_001));

        CardSorting cardSorting = CardSorting.create(question, request.categories());
        cardSortingRepository.save(cardSorting);
    }
}
