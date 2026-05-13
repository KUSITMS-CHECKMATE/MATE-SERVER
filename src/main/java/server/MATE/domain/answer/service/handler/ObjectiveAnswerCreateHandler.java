package server.MATE.domain.answer.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.ObjectiveAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ObjectiveAnswerCreateHandler implements AnswerCreateHandler {

    private final ObjectiveRepository objectiveRepository;
    private final AnswerRepository answerRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.OBJECTIVE;
    }

    @Override
    public Answer save(Long participationId, AnswerCreateItem item) {
        ObjectiveAnswerCreateRequest request = (ObjectiveAnswerCreateRequest) item;

        Objective objective = objectiveRepository.findWithOptionsById(request.questionId())
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        Set<Long> validOptionIds = objective.getOptions().stream()
                .map(ObjectiveOption::getId)
                .collect(Collectors.toSet());

        List<Long> selectedOptionIds = request.optionIds();
        boolean hasOtherText = objective.isOther() && request.otherText() != null && !request.otherText().isBlank();

        if (selectedOptionIds.isEmpty() && !hasOtherText) {
            throw new BaseException(BaseErrorCode.ANSWER_005);
        }

        if (!objective.isOther() && request.otherText() != null && !request.otherText().isBlank()) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }

        validateSelectedOptions(selectedOptionIds, validOptionIds);

        int selectedCount = selectedOptionIds.size();
        if (!objective.isDuplicate()) {
            if (hasOtherText && selectedCount > 0) throw new BaseException(BaseErrorCode.ANSWER_006);
            if (!hasOtherText && selectedCount != 1) throw new BaseException(BaseErrorCode.ANSWER_006);
        } else {
            int min = objective.getMinSelect() != null ? objective.getMinSelect() : 1;
            int max = objective.getMaxSelect() != null ? objective.getMaxSelect() : validOptionIds.size() + (objective.isOther() ? 1 : 0);
            int effectiveCount = selectedCount + (hasOtherText ? 1 : 0);
            if (effectiveCount < min || effectiveCount > max) throw new BaseException(BaseErrorCode.ANSWER_006);
        }

        Map<String, Object> answerMap = new LinkedHashMap<>();
        answerMap.put("optionIds", selectedOptionIds);
        if (hasOtherText) answerMap.put("otherText", request.otherText().trim());

        Answer answer = Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.OBJECTIVE)
                .answer(answerMap)
                .build();
        return answerRepository.save(answer);
    }

    private void validateSelectedOptions(List<Long> selectedOptionIds, Set<Long> validOptionIds) {
        if (selectedOptionIds.size() != Set.copyOf(selectedOptionIds).size()) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }
        if (!validOptionIds.containsAll(selectedOptionIds)) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }
    }
}
