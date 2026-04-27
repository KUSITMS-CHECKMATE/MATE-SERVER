package server.MATE.question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import server.MATE.question.dto.cardCategoryCreateRequest;
import server.MATE.question.entity.question;
import server.MATE.question.repository.questionRepository;

@Service
@RequiredArgsConstructor
public class questionService {

    private final questionRepository questionRepository;

    @Transactional
    public void cardSortingCreation(cardCategoryCreateRequest request) {
        question cardSortingQuestion = question.createCardSortingQuestion(
                request.testId(),
                request.title(),
                request.description(),
                request.sequence(),
                request.categories()
        );
        questionRepository.save(Objects.requireNonNull(cardSortingQuestion));
    }
}
