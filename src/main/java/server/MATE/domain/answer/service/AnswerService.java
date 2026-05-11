package server.MATE.domain.answer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.dto.request.SubjectiveAnswerCreateRequest;
import server.MATE.domain.answer.dto.response.AnswerCreateResponse;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnswerService {

    private final ParticipationRepository participationRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AnswerCreateResponse createSubjectiveAnswer(Long participationId, SubjectiveAnswerCreateRequest request) {
        Participation participation = participationRepository.findByIdAndDeletedAtIsNull(participationId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PARTICIPATION_001));

        Question question = questionRepository.findByIdAndDeletedAtIsNull(request.questionId())
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        if (question.getQuestionType() != QuestionType.SUBJECTIVE) {
            throw new BaseException(BaseErrorCode.ANSWER_001);
        }

        if (!question.getTestId().equals(participation.getTestId())) {
            throw new BaseException(BaseErrorCode.ANSWER_002);
        }

        String answerJson = toJson(Map.of("text", request.text()));

        Answer answer = Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.SUBJECTIVE)
                .answer(answerJson)
                .build();

        answerRepository.save(answer);
        return AnswerCreateResponse.from(answer);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BaseException(BaseErrorCode.COMMON_999);
        }
    }
}
