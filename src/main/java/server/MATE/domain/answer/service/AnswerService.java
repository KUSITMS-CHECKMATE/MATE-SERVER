package server.MATE.domain.answer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.dto.request.ObjectiveAnswerCreateRequest;
import server.MATE.domain.answer.dto.request.SubjectiveAnswerCreateRequest;
import server.MATE.domain.answer.dto.response.AnswerCreateResponse;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
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
    private final AnswerRepository answerRepository;

    @Transactional
    public AnswerCreateResponse createSubjectiveAnswer(Long participationId, Long testerId, SubjectiveAnswerCreateRequest request) {
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

    @Transactional
    public AnswerCreateResponse createObjectiveAnswer(Long participationId, Long testerId, ObjectiveAnswerCreateRequest request) {
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

        if (selectedOptionIds.size() != Set.copyOf(selectedOptionIds).size()) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }

        if (!validOptionIds.containsAll(selectedOptionIds)) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }

        int selectedCount = selectedOptionIds.size();
        if (!objective.isDuplicate()) {
            // 단일 선택: 일반 선택지 1개 또는 기타만 선택 가능
            if (hasOtherText && selectedCount > 0) {
                throw new BaseException(BaseErrorCode.ANSWER_005);
            }
            if (!hasOtherText && selectedCount != 1) {
                throw new BaseException(BaseErrorCode.ANSWER_005);
            }
        } else {
            int min = objective.getMinSelect() != null ? objective.getMinSelect() : 1;
            int max = objective.getMaxSelect() != null ? objective.getMaxSelect() : validOptionIds.size();
            int effectiveCount = selectedCount + (hasOtherText ? 1 : 0);
            if (effectiveCount < min || effectiveCount > max) {
                throw new BaseException(BaseErrorCode.ANSWER_005);
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
