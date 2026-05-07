package server.MATE.domain.question.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.request.ObjectiveCreateRequest;
import server.MATE.domain.question.dto.request.ObjectiveOptionRequest;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;

@Component
public class ObjectiveQuestionCreateHandler extends AbstractQuestionCreateHandler<ObjectiveCreateRequest> {

    private final ObjectiveRepository objectiveRepository;

    public ObjectiveQuestionCreateHandler(ObjectiveRepository objectiveRepository) {
        super(ObjectiveCreateRequest.class);
        this.objectiveRepository = objectiveRepository;
    }

    @Override
    public QuestionType supports() {
        return QuestionType.OBJECTIVE;
    }

    @Override
    protected void validateTyped(ObjectiveCreateRequest item) {
        // 객관식 선택지는 최소 2개, 최대 10개까지 허용함
        int optionCount = item.options() == null ? 0 : item.options().size();
        if (optionCount < 2 || optionCount > 10) {
            throw new BaseException(BaseErrorCode.COMMON_002);
        }

        // 단일 선택 객관식에는 min/max 선택 개수를 사용할 수 없음
        if (!item.isDuplicate()) {
            if (item.minSelect() != null || item.maxSelect() != null) {
                throw new BaseException(BaseErrorCode.QUESTION_009);
            }
            return;
        }

        Integer min = item.minSelect();
        Integer max = item.maxSelect();

        // 최소 선택 개수는 1 이상이어야 함
        if (min != null && min < 1) {
            throw new BaseException(BaseErrorCode.QUESTION_001);
        }

        // 최대 선택 개수는 최소 선택 개수보다 작을 수 없음
        if (min != null && max != null && max < min) {
            throw new BaseException(BaseErrorCode.QUESTION_002);
        }

        // min/max 선택 개수는 전체 선택지 개수를 초과할 수 없음
        if (max != null && max > optionCount) {
            throw new BaseException(BaseErrorCode.QUESTION_003);
        }
        if (min != null && min > optionCount) {
            throw new BaseException(BaseErrorCode.QUESTION_003);
        }
    }

    @Override
    protected void createDetailTyped(Question question, ObjectiveCreateRequest item) {
        Integer maxSelect = item.isDuplicate() ? item.maxSelect() : null;
        Integer minSelect = item.isDuplicate() ? item.minSelect() : null;

        Objective objective = Objective.builder()
                .question(question)
                .isDuplicate(item.isDuplicate())
                .maxSelect(maxSelect)
                .minSelect(minSelect)
                .isOther(item.isOther())
                .build();

        List<ObjectiveOptionRequest> optionRequests = item.options();
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

        objectiveRepository.save(objective);
    }

    @Override
    protected List<String> extractImageKeysTyped(ObjectiveCreateRequest item) {
        return item.options().stream()
                .map(ObjectiveOptionRequest::imageKey)
                .filter(key -> key != null && !key.isBlank())
                .toList();
    }
}
