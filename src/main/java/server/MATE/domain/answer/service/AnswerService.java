package server.MATE.domain.answer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.FiveSecondAnswerCreateRequest;
import server.MATE.domain.answer.dto.request.ObjectiveAnswerCreateRequest;
import server.MATE.domain.answer.dto.request.SubjectiveAnswerCreateRequest;
import server.MATE.domain.answer.dto.response.AnswerCreateResponse;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.FiveSecondOption;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnswerService {

    private final ParticipationRepository participationRepository;
    private final QuestionRepository questionRepository;
    private final ObjectiveRepository objectiveRepository;
    private final FiveSecondRepository fiveSecondRepository;
    private final AnswerRepository answerRepository;

    @Transactional
    public AnswerCreateResponse createAnswer(Long participationId, Long testerId, AnswerCreateItem request) {
        return switch (request) {
            case SubjectiveAnswerCreateRequest r -> createSubjectiveAnswer(participationId, testerId, r);
            case ObjectiveAnswerCreateRequest r -> createObjectiveAnswer(participationId, testerId, r);
            case FiveSecondAnswerCreateRequest r -> createFiveSecondAnswer(participationId, testerId, r);
        };
    }

    private AnswerCreateResponse createSubjectiveAnswer(Long participationId, Long testerId, SubjectiveAnswerCreateRequest request) {
        validateAnswerRequest(participationId, testerId, request.questionId(), QuestionType.SUBJECTIVE);

        Answer answer = Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.SUBJECTIVE)
                .answer(Map.of("text", request.text()))
                .build();

        answerRepository.save(answer);
        return AnswerCreateResponse.from(answer);
    }

    private AnswerCreateResponse createObjectiveAnswer(Long participationId, Long testerId, ObjectiveAnswerCreateRequest request) {
        validateAnswerRequest(participationId, testerId, request.questionId(), QuestionType.OBJECTIVE);

        Objective objective = objectiveRepository.findWithOptionsById(request.questionId())
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        Set<Long> validOptionIds = objective.getOptions().stream()
                .map(ObjectiveOption::getId)
                .collect(Collectors.toSet());

        List<Long> selectedOptionIds = request.selectedOptionIds();
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
            if (hasOtherText && selectedCount > 0) {
                throw new BaseException(BaseErrorCode.ANSWER_006);
            }
            if (!hasOtherText && selectedCount != 1) {
                throw new BaseException(BaseErrorCode.ANSWER_006);
            }
        } else {
            int min = objective.getMinSelect() != null ? objective.getMinSelect() : 1;
            int max = objective.getMaxSelect() != null ? objective.getMaxSelect() : validOptionIds.size() + (objective.isOther() ? 1 : 0);
            int effectiveCount = selectedCount + (hasOtherText ? 1 : 0);
            if (effectiveCount < min || effectiveCount > max) {
                throw new BaseException(BaseErrorCode.ANSWER_006);
            }
        }

        Map<String, Object> answerMap = new LinkedHashMap<>();
        answerMap.put("selectedOptionIds", selectedOptionIds);
        if (hasOtherText) {
            answerMap.put("otherText", request.otherText().trim());
        }

        Answer answer = Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.OBJECTIVE)
                .answer(answerMap)
                .build();

        answerRepository.save(answer);
        return AnswerCreateResponse.from(answer);
    }

    private AnswerCreateResponse createFiveSecondAnswer(Long participationId, Long testerId, FiveSecondAnswerCreateRequest request) {
        validateAnswerRequest(participationId, testerId, request.questionId(), QuestionType.FIVE_SECOND);

        FiveSecond fiveSecond = fiveSecondRepository.findWithOptionsById(request.questionId())
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        Map<String, Object> answerMap;

        if (!fiveSecond.isObjective()) {
            if (request.text() == null || request.text().isBlank()) {
                throw new BaseException(BaseErrorCode.ANSWER_005);
            }
            if (request.selectedOptionIds() != null && !request.selectedOptionIds().isEmpty()) {
                throw new BaseException(BaseErrorCode.ANSWER_004);
            }
            answerMap = Map.of("text", request.text().trim());
        } else {
            if (request.text() != null && !request.text().isBlank()) {
                throw new BaseException(BaseErrorCode.ANSWER_004);
            }
            List<Long> selectedOptionIds = request.selectedOptionIds() != null ? request.selectedOptionIds() : List.of();

            Set<Long> validOptionIds = fiveSecond.getOptions().stream()
                    .map(FiveSecondOption::getId)
                    .collect(Collectors.toSet());

            if (selectedOptionIds.isEmpty()) {
                throw new BaseException(BaseErrorCode.ANSWER_005);
            }

            validateSelectedOptions(selectedOptionIds, validOptionIds);

            int selectedCount = selectedOptionIds.size();
            if (!Boolean.TRUE.equals(fiveSecond.getIsDuplicate())) {
                if (selectedCount != 1) {
                    throw new BaseException(BaseErrorCode.ANSWER_006);
                }
            } else {
                int min = fiveSecond.getMinSelect() != null ? fiveSecond.getMinSelect() : 1;
                int max = fiveSecond.getMaxSelect() != null ? fiveSecond.getMaxSelect() : validOptionIds.size();
                if (selectedCount < min || selectedCount > max) {
                    throw new BaseException(BaseErrorCode.ANSWER_006);
                }
            }

            answerMap = Map.of("selectedOptionIds", selectedOptionIds);
        }

        Answer answer = Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.FIVE_SECOND)
                .answer(answerMap)
                .build();

        answerRepository.save(answer);
        return AnswerCreateResponse.from(answer);
    }

    private void validateSelectedOptions(List<Long> selectedOptionIds, Set<Long> validOptionIds) {
        if (selectedOptionIds.size() != Set.copyOf(selectedOptionIds).size()) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }
        if (!validOptionIds.containsAll(selectedOptionIds)) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }
    }

    private void validateAnswerRequest(Long participationId, Long testerId, Long questionId, QuestionType expectedType) {
        Participation participation = participationRepository.findByIdAndDeletedAtIsNull(participationId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PARTICIPATION_001));

        participation.validateTester(testerId);

        Question question = questionRepository.findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        if (question.getQuestionType() != expectedType) {
            throw new BaseException(BaseErrorCode.ANSWER_001);
        }

        question.validateTestBelonging(participation.getTestId());

        if (answerRepository.existsByParticipationIdAndQuestionIdAndDeletedAtIsNull(participationId, questionId)) {
            throw new BaseException(BaseErrorCode.ANSWER_003);
        }
    }

}
