package server.MATE.domain.answer.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.FiveSecondAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.service.AnswerCreateContext;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.FiveSecondOption;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FiveSecondAnswerCreateHandler implements AnswerCreateHandler {

    @Override
    public QuestionType supports() {
        return QuestionType.FIVE_SECOND;
    }

    @Override
    public void validate(AnswerCreateItem item, AnswerCreateContext context) {
        FiveSecondAnswerCreateRequest request = (FiveSecondAnswerCreateRequest) item;

        FiveSecond fiveSecond = context.fiveSeconds().get(request.questionId());
        if (fiveSecond == null) throw new BaseException(BaseErrorCode.QUESTION_005);

        if (!fiveSecond.isObjective()) {
            // 주관식 5초 테스트 응답일 때, text만 허용함
            if (request.text() == null || request.text().isBlank()) throw new BaseException(BaseErrorCode.ANSWER_005);
            if (request.optionIds() != null && !request.optionIds().isEmpty()) throw new BaseException(BaseErrorCode.ANSWER_004);
            if (request.otherText() != null && !request.otherText().isBlank()) throw new BaseException(BaseErrorCode.ANSWER_004);
        } else {
            // 객관식 5초 테스트 응답일 때, optionIds 기준으로 응답해야 함
            if (request.text() != null && !request.text().isBlank()) throw new BaseException(BaseErrorCode.ANSWER_004);
            List<Long> selectedOptionIds = request.optionIds() != null ? request.optionIds() : List.of();

            Set<Long> validOptionIds = fiveSecond.getOptions().stream()
                    .map(FiveSecondOption::getId)
                    .collect(Collectors.toSet());

            Long otherOptionId = fiveSecond.getOptions().stream()
                    .filter(option -> Boolean.TRUE.equals(option.getIsOtherOption()))
                    .map(FiveSecondOption::getId)
                    .findFirst()
                    .orElse(null);

            boolean hasOtherText = request.otherText() != null && !request.otherText().isBlank();
            boolean selectedOtherOption = otherOptionId != null && selectedOptionIds.contains(otherOptionId);

            // 객관식 5초 테스트 응답은 최소 1개 이상의 옵션(선지)를 가져야 함
            if (selectedOptionIds.isEmpty()) throw new BaseException(BaseErrorCode.ANSWER_005);

            // 기타 option 선택 여부와 otherText 입력 여부가 일치해야 함
            if (!Boolean.TRUE.equals(fiveSecond.getIsOther()) && (hasOtherText || selectedOtherOption)) {
                throw new BaseException(BaseErrorCode.ANSWER_004);
            }

            // 기타 option 선택 시, otherText를 가져야 함
            if (selectedOtherOption != hasOtherText) {
                throw new BaseException(BaseErrorCode.ANSWER_004);
            }

            // 질문의 옵션(선지)만 중복 없이 선택해야 함
            if (selectedOptionIds.size() != Set.copyOf(selectedOptionIds).size()) {
                throw new BaseException(BaseErrorCode.ANSWER_004);
            }
            if (!validOptionIds.containsAll(selectedOptionIds)) {
                throw new BaseException(BaseErrorCode.ANSWER_004);
            }

            int selectedCount = selectedOptionIds.size();

            // 단일/복수 선택 설정에 맞는 선택 개수만 허용함
            if (!Boolean.TRUE.equals(fiveSecond.getIsDuplicate())) {
                if (selectedCount != 1) throw new BaseException(BaseErrorCode.ANSWER_006);
            } else {
                int min = fiveSecond.getMinSelect() != null ? fiveSecond.getMinSelect() : 1;
                int max = fiveSecond.getMaxSelect() != null ? fiveSecond.getMaxSelect() : validOptionIds.size();
                if (selectedCount < min || selectedCount > max) throw new BaseException(BaseErrorCode.ANSWER_006);
            }
        }
    }

    @Override
    public Answer build(Long participationId, AnswerCreateItem item, AnswerCreateContext context) {
        FiveSecondAnswerCreateRequest request = (FiveSecondAnswerCreateRequest) item;
        FiveSecond fiveSecond = context.fiveSeconds().get(request.questionId());
        Map<String, Object> answerMap;

        if (!fiveSecond.isObjective()) {
            answerMap = Map.of("text", request.text().trim());
        } else {
            boolean hasOtherText = request.otherText() != null && !request.otherText().isBlank();
            answerMap = new LinkedHashMap<>();
            answerMap.put("optionIds", request.optionIds() != null ? request.optionIds() : List.of());
            if (hasOtherText) {
                answerMap.put("otherText", request.otherText().trim());
            }
        }

        return Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.FIVE_SECOND)
                .answer(answerMap)
                .build();
    }
}
