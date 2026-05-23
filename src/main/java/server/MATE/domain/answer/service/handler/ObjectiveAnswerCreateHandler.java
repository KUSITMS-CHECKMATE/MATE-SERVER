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
    public void validate(AnswerCreateItem item, AnswerCreateContext context) {
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

        // 객관식 응답은 최소 1개 이상의 옵션(선지)가 필요함
        if (selectedOptionIds.isEmpty()) {
            throw new BaseException(BaseErrorCode.ANSWER_005);
        }

        // 기타 option 선택 여부와 otherText 입력 여부가 일치해야 함
        if (!objective.isOther() && (hasOtherText || selectedOtherOption)) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }

        // 기타 option 선택 시, otherText를 가져야 함
        if (selectedOtherOption != hasOtherText) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }

        // 질문의 옵션(선지)만 중복 없이 선택해야 함
        validateSelectedOptions(selectedOptionIds, validOptionIds);

        int selectedCount = selectedOptionIds.size();

        // 단일/복수 선택 설정에 맞는 선택 개수만 허용함
        if (!objective.isDuplicate()) {
            if (selectedCount != 1) throw new BaseException(BaseErrorCode.ANSWER_006);
        } else {
            int min = objective.getMinSelect() != null ? objective.getMinSelect() : 1;
            int max = objective.getMaxSelect() != null ? objective.getMaxSelect() : validOptionIds.size();
            if (selectedCount < min || selectedCount > max) throw new BaseException(BaseErrorCode.ANSWER_006);
        }
    }

    @Override
    public Answer build(Long participationId, AnswerCreateItem item, AnswerCreateContext context) {
        ObjectiveAnswerCreateRequest request = (ObjectiveAnswerCreateRequest) item;
        boolean hasOtherText = request.otherText() != null && !request.otherText().isBlank();
        Map<String, Object> answerMap = new LinkedHashMap<>();
        answerMap.put("optionIds", request.optionIds());
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
