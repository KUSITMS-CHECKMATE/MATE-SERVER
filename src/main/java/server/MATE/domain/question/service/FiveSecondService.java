package server.MATE.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.request.FiveSecondCreateRequest;
import server.MATE.domain.question.dto.request.FiveSecondOptionRequest;
import server.MATE.domain.question.dto.response.FiveSecondCreateResponse;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.FiveSecondOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.FiveSecondRepository;
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
public class FiveSecondService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final FiveSecondRepository fiveSecondRepository;
    private final ApplicationEventPublisher eventPublisher;

    public FiveSecondCreateResponse createFiveSecond(Long testId, Long makerId, FiveSecondCreateRequest request) {
        Test test = testRepository.findById(testId)
                .filter(t -> t.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.TEST_005);
        }

        validateRequest(request);

        // TODO: 동시성 이슈 - 현재 MAX(sequence)+1 방식은 동시 요청 시 중복 순서값 발생 가능.
        //  별도 시퀀스 관리 로직 개발 후 수정 예정.
        Long sequence = questionRepository.findMaxSequenceByTestId(testId) + 1;

        eventPublisher.publishEvent(new ImageCleanupEvent(List.of(request.imageKey())));

        Question question = Question.builder()
                .testId(testId)
                .questionType(QuestionType.FIVE_SECOND)
                .title(request.title())
                .description(request.description())
                .sequence(sequence)
                .build();
        questionRepository.save(question);

        boolean isDuplicate = Boolean.TRUE.equals(request.isDuplicate());
        Integer minSelect = (request.isObjective() && isDuplicate) ? request.minSelect() : null;
        Integer maxSelect = (request.isObjective() && isDuplicate) ? request.maxSelect() : null;

        FiveSecond fiveSecond = FiveSecond.builder()
                .question(question)
                .imageKey(request.imageKey())
                .isObjective(request.isObjective())
                .isDuplicate(request.isObjective() ? request.isDuplicate() : null)
                .minSelect(minSelect)
                .maxSelect(maxSelect)
                .build();

        if (request.isObjective() && request.options() != null) {
            List<FiveSecondOptionRequest> optionRequests = request.options();
            for (int i = 0; i < optionRequests.size(); i++) {
                FiveSecondOption option = FiveSecondOption.builder()
                        .fiveSecond(fiveSecond)
                        .content(optionRequests.get(i).content())
                        .sequence(i + 1)
                        .build();
                fiveSecond.addOption(option);
            }
        }

        fiveSecondRepository.save(fiveSecond);

        return FiveSecondCreateResponse.from(fiveSecond);
    }

    private void validateRequest(FiveSecondCreateRequest request) {
        if (!request.isObjective()) return;

        List<FiveSecondOptionRequest> options = request.options();
        if (options == null || options.size() < 2) {
            throw new BaseException(BaseErrorCode.QUESTION_004);
        }

        if (!Boolean.TRUE.equals(request.isDuplicate())) return;

        int optionCount = options.size();
        Integer min = request.minSelect();
        Integer max = request.maxSelect();

        if (min != null && min < 1) {
            throw new BaseException(BaseErrorCode.QUESTION_001);
        }
        if (min != null && max != null && max < min) {
            throw new BaseException(BaseErrorCode.QUESTION_002);
        }
        if (min != null && min > optionCount) {
            throw new BaseException(BaseErrorCode.QUESTION_003);
        }
        if (max != null && max > optionCount) {
            throw new BaseException(BaseErrorCode.QUESTION_003);
        }
    }
}
