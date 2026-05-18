package server.MATE.domain.answer.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.ObjectiveAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.service.AnswerCreateContext;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ObjectiveAnswerCreateHandler implements AnswerCreateHandler {

    @Override
    public QuestionType supports() {
        return QuestionType.OBJECTIVE;
    }

    @Override
    public Answer build(Long participationId, AnswerCreateItem item, AnswerCreateContext context) {
        ObjectiveAnswerCreateRequest request = (ObjectiveAnswerCreateRequest) item;

        Objective objective = context.objectives().get(request.questionId());
        if (objective == null) throw new BaseException(BaseErrorCode.QUESTION_005);

        Set<Long> validOptionIds = objective.getOptions().stream()
                .map(ObjectiveOption::getId)
                .collect(Collectors.toSet());

        Long otherOptionId = objective.getOptions().stream()
                .filter(option -> Boolean.TRUE.equals(option.getIsOtherOption()))
                .map(ObjectiveOption::getId)
                .findFirst()
                .orElse(null);

        List<Long> selectedOptionIds = request.optionIds();
        boolean hasOtherText = request.otherText() != null && !request.otherText().isBlank();
        boolean selectedOtherOption = otherOptionId != null && selectedOptionIds.contains(otherOptionId);

        // 객관식 응답은 최소 1개 이상의 선택지가 필요함
        if (selectedOptionIds.isEmpty()) {
            throw new BaseException(BaseErrorCode.ANSWER_005);
        }

        // 기타 선택지를 허용하지 않는 문항에는 기타 option, otherText를 함께 사용할 수 없음
        if (!objective.isOther() && (hasOtherText || selectedOtherOption)) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }

        // 기타 option 선택 여부와 otherText 입력 여부는 반드시 함께 일치해야 함
        if (selectedOtherOption != hasOtherText) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }

        validateSelectedOptions(selectedOptionIds, validOptionIds);

        int selectedCount = selectedOptionIds.size();
        // 단일 선택 객관식은 정확히 1개의 선택지만 허용함
        if (!objective.isDuplicate()) {
            if (selectedCount != 1) throw new BaseException(BaseErrorCode.ANSWER_006);
        } else {
            int min = objective.getMinSelect() != null ? objective.getMinSelect() : 1;
            int max = objective.getMaxSelect() != null ? objective.getMaxSelect() : validOptionIds.size();
            // 복수 선택 객관식은 min/max 범위 내에서만 선택할 수 있음
            if (selectedCount < min || selectedCount > max) throw new BaseException(BaseErrorCode.ANSWER_006);
        }

        Map<String, Object> answerMap = new LinkedHashMap<>();
        answerMap.put("optionIds", selectedOptionIds);
        if (hasOtherText) answerMap.put("otherText", request.otherText().trim());

        return Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.OBJECTIVE)
                .answer(answerMap)
                .build();
    }

    private void validateSelectedOptions(List<Long> selectedOptionIds, Set<Long> validOptionIds) {
        // 동일한 선택지를 중복 선택할 수 없음
        if (selectedOptionIds.size() != Set.copyOf(selectedOptionIds).size()) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }
        // 현재 문항에 존재하지 않는 선택지는 응답할 수 없음
        if (!validOptionIds.containsAll(selectedOptionIds)) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }
    }
}
