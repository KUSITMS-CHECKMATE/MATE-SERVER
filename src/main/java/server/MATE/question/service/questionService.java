package server.MATE.question.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.ErrorCode;
import server.MATE.question.dto.cardCategoryCreateRequest;
import server.MATE.question.dto.treeTestCreateRequest;
import server.MATE.question.entity.Question;
import server.MATE.question.repository.questionRepository;
import server.MATE.test.entity.Test;
import server.MATE.test.repository.testRepository;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class questionService {

    private final questionRepository questionRepository;
    private final testRepository testRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void cardSortingCreation(cardCategoryCreateRequest request) {
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

    @Transactional
    public void treeTestCreation(treeTestCreateRequest request) {
        Long testId = Objects.requireNonNull(request.testId());
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BaseException(ErrorCode.TEST_001));

        JsonNode branch = objectMapper.valueToTree(request.branches());
        Question treeTestQuestion = Question.createTreeTestQuestion(
                test,
                request.title(),
                request.description(),
                request.sequence(),
                branch
        );
        questionRepository.save(Objects.requireNonNull(treeTestQuestion));
    }
}
