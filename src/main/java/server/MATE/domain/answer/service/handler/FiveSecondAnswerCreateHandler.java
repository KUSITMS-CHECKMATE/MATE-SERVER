package server.MATE.domain.answer.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.FiveSecondAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.FiveSecondOption;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FiveSecondAnswerCreateHandler implements AnswerCreateHandler {

    private final FiveSecondRepository fiveSecondRepository;
    private final AnswerRepository answerRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.FIVE_SECOND;
    }

    @Override
    public Answer save(Long participationId, AnswerCreateItem item) {
        FiveSecondAnswerCreateRequest request = (FiveSecondAnswerCreateRequest) item;

        FiveSecond fiveSecond = fiveSecondRepository.findWithOptionsById(request.questionId())
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        Map<String, Object> answerMap;

        if (!fiveSecond.isObjective()) {
            if (request.text() == null || request.text().isBlank()) throw new BaseException(BaseErrorCode.ANSWER_005);
            if (request.optionIds() != null && !request.optionIds().isEmpty()) throw new BaseException(BaseErrorCode.ANSWER_004);
            answerMap = Map.of("text", request.text().trim());
        } else {
            if (request.text() != null && !request.text().isBlank()) throw new BaseException(BaseErrorCode.ANSWER_004);
            List<Long> selectedOptionIds = request.optionIds() != null ? request.optionIds() : List.of();

            Set<Long> validOptionIds = fiveSecond.getOptions().stream()
                    .map(FiveSecondOption::getId)
                    .collect(Collectors.toSet());

            if (selectedOptionIds.isEmpty()) throw new BaseException(BaseErrorCode.ANSWER_005);

            validateSelectedOptions(selectedOptionIds, validOptionIds);

            int selectedCount = selectedOptionIds.size();
            if (!Boolean.TRUE.equals(fiveSecond.getIsDuplicate())) {
                if (selectedCount != 1) throw new BaseException(BaseErrorCode.ANSWER_006);
            } else {
                int min = fiveSecond.getMinSelect() != null ? fiveSecond.getMinSelect() : 1;
                int max = fiveSecond.getMaxSelect() != null ? fiveSecond.getMaxSelect() : validOptionIds.size();
                if (selectedCount < min || selectedCount > max) throw new BaseException(BaseErrorCode.ANSWER_006);
            }

            answerMap = Map.of("optionIds", selectedOptionIds);
        }

        Answer answer = Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.FIVE_SECOND)
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
