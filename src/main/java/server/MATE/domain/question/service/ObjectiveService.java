package server.MATE.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.request.ObjectiveOptionRequest;
import server.MATE.domain.question.dto.request.ObjectiveCreateRequest;
import server.MATE.domain.question.dto.response.ObjectiveCreateResponse;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.image.event.ImageCleanupEvent;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ObjectiveService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final ObjectiveRepository objectiveRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ObjectiveCreateResponse createObjective(
            Long testId,
            ObjectiveCreateRequest request
    ) {
        if (!testRepository.existsById(testId)) {
            throw new BaseException(BaseErrorCode.TEST_004);
        }

        validateSelectRange(request);

        // TODO: 동시성 이슈 - 현재 MAX(sequence)+1 방식은 동시 요청 시 중복 순서값 발생 가능.
        //  별도 시퀀스 관리 로직 개발 후 수정 예정.
        Long sequence = questionRepository.findMaxSequenceByTestId(testId) + 1;

        Question question = Question.builder()
                .testId(testId)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .title(request.title())
                .description(request.description())
                .sequence(sequence)
                .build();
        questionRepository.save(question);

        Integer maxSelect = request.isDuplicate() ? request.maxSelect() : null;
        Integer minSelect = request.isDuplicate() ? request.minSelect() : null;

        Objective objective = Objective.builder()
                .question(question)
                .isDuplicate(request.isDuplicate())
                .maxSelect(maxSelect)
                .minSelect(minSelect)
                .isOther(request.isOther())
                .build();

        List<ObjectiveOptionRequest> optionRequests = request.options();
        for (int i = 0; i < optionRequests.size(); i++) {
            ObjectiveOptionRequest optionRequest = optionRequests.get(i);
            ObjectiveOption option = ObjectiveOption.builder()
                    .objective(objective)
                    .content(optionRequest.content())
                    .imageKey(optionRequest.imageKey())
                    .sequence(i + 1)
                    .build();
            objective.addOption(option);
        }

        List<String> imageKeys = optionRequests.stream()
                .map(ObjectiveOptionRequest::imageKey)
                .filter(key -> key != null)
                .toList();
        if (!imageKeys.isEmpty()) {
            eventPublisher.publishEvent(new ImageCleanupEvent(imageKeys));
        }

        objectiveRepository.save(objective);

        return ObjectiveCreateResponse.from(objective);
    }

    private void validateSelectRange(ObjectiveCreateRequest request) {
        if (!request.isDuplicate()) return;

        Integer min = request.minSelect();
        Integer max = request.maxSelect();
        int optionCount = request.options().size();

        if (min != null && min < 1) {
            throw new BaseException(BaseErrorCode.QUESTION_001);
        }
        if (min != null && max != null && max < min) {
            throw new BaseException(BaseErrorCode.QUESTION_002);
        }
        if (max != null && max > optionCount) {
            throw new BaseException(BaseErrorCode.QUESTION_003);
        }
        if (min != null && min > optionCount) {
            throw new BaseException(BaseErrorCode.QUESTION_003);
        }
    }
}
