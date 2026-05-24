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

    private static final String OTHER_OPTION_CONTENT = "기타 (직접 입력)";

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
        int optionCount = item.options() == null ? 0 : item.options().size();
        int effectiveOptionCount = optionCount + (item.isOther() ? 1 : 0);

        // 객관식은 기본 옵션(선지)을 최소 2개 이상 가져야 하며, 기타 포함 총 옵션(선지)은 10개를 초과할 수 없음
        if (optionCount < 2 || effectiveOptionCount > 10) {
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

        // min/max 선택 개수는 전체 옵션(선지) 개수를 초과할 수 없음
        if (max != null && max > effectiveOptionCount) {
            throw new BaseException(BaseErrorCode.QUESTION_003);
        }
        if (min != null && min > effectiveOptionCount) {
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
                    .isOtherOption(false)
                    .build();
            objective.addOption(option);
        }

        if (item.isOther()) {
            ObjectiveOption otherOption = ObjectiveOption.builder()
                    .objective(objective)
                    .content(OTHER_OPTION_CONTENT)
                    .imageKey(null)
                    .sequence(optionRequests.size() + 1)
                    .isOtherOption(true)
                    .build();
            objective.addOption(otherOption);
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
