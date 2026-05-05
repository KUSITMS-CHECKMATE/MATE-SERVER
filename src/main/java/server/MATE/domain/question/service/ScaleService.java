package server.MATE.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.request.ScaleCreateRequest;
import server.MATE.domain.question.dto.response.ScaleCreateResponse;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.image.event.ImageCleanupEvent;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ScaleService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final ScaleRepository scaleRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ScaleCreateResponse createScale(Long testId, Long makerId, ScaleCreateRequest request) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.COMMON_009);
        }

        validateScaleRange(request.range());

        // TODO: 동시성 이슈 - 현재 MAX(sequence)+1 방식은 동시 요청 시 중복 순서값 발생 가능.
        //  별도 시퀀스 관리 로직 개발 후 수정 예정.
        Long sequence = questionRepository.findMaxSequenceByTestId(testId) + 1;

        Question question = Question.builder()
                .testId(testId)
                .questionType(QuestionType.SCALE)
                .title(request.title())
                .description(request.description())
                .sequence(sequence)
                .build();

        Scale scale = Scale.builder()
                .question(question)
                .imageKey(request.imageKey())
                .minLabel(request.minLabel())
                .maxLabel(request.maxLabel())
                .range(request.range())
                .build();

        if (request.imageKey() != null) {
            eventPublisher.publishEvent(new ImageCleanupEvent(List.of(request.imageKey())));
        }

        questionRepository.save(question);
        scaleRepository.save(scale);

        return ScaleCreateResponse.from(scale);
    }

    private void validateScaleRange(Integer range) {
        if (range != 5 && range != 7) {
            throw new BaseException(BaseErrorCode.COMMON_002);
        }
    }
}
