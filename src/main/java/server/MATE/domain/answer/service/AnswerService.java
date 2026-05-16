package server.MATE.domain.answer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.response.AnswerCreateResponse;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.answer.service.handler.AnswerCreateHandler;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AnswerService {

    private final ParticipationRepository participationRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final Map<QuestionType, AnswerCreateHandler> handlerMap;

    public AnswerService(ParticipationRepository participationRepository,
                         QuestionRepository questionRepository,
                         AnswerRepository answerRepository,
                         List<AnswerCreateHandler> handlers) {
        this.participationRepository = participationRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.handlerMap = buildHandlerMap(handlers);
    }

    @Transactional
    public AnswerCreateResponse createAnswer(Long participationId, Long testerId, AnswerCreateItem request) {
        validateAnswerRequest(participationId, testerId, request.questionId(), request.type());
        AnswerCreateHandler handler = handlerMap.get(request.type());
        Answer answer = handler.save(participationId, request);
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

    private Map<QuestionType, AnswerCreateHandler> buildHandlerMap(List<AnswerCreateHandler> handlers) {
        Map<QuestionType, AnswerCreateHandler> map = new EnumMap<>(QuestionType.class);
        for (AnswerCreateHandler handler : handlers) {
            map.put(handler.supports(), handler);
        }
        return map;
    }
}
