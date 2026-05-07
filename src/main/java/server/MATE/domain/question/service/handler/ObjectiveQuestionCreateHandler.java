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
        if (!item.isDuplicate()) {
            return;
        }

        Integer min = item.minSelect();
        Integer max = item.maxSelect();
        int optionCount = item.options().size();

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
