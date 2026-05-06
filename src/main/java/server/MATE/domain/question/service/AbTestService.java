package server.MATE.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.request.AbTestCreateRequest;
import server.MATE.domain.question.dto.response.AbTestCreateResponse;
import server.MATE.domain.question.entity.AbTest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.AbTestRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.image.event.ImageCleanupEvent;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AbTestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final AbTestRepository abTestRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AbTestCreateResponse createAbTest(Long testId, Long makerId, AbTestCreateRequest request) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.COMMON_009);
        }

        // TODO: 동시성 이슈 - 현재 MAX(sequence)+1 방식은 동시 요청 시 중복 순서값 발생 가능.
        //  별도 시퀀스 관리 로직 개발 후 수정 예정.
        Long sequence = questionRepository.findMaxSequenceByTestId(testId) + 1;

        Question question = Question.builder()
                .testId(testId)
                .questionType(QuestionType.AB_TEST)
                .title(request.title())
                .description(request.description())
                .sequence(sequence)
                .build();

        AbTest abTest = AbTest.builder()
                .question(question)
                .aImageKey(request.aImageKey())
                .bImageKey(request.bImageKey())
                .build();

        eventPublisher.publishEvent(new ImageCleanupEvent(
                List.of(request.aImageKey(), request.bImageKey())
        ));

        questionRepository.save(question);
        abTestRepository.save(abTest);

        return AbTestCreateResponse.from(abTest);
    }
}
