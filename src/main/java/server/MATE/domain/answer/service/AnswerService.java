package server.MATE.domain.answer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.AnswerCreateRequest;
import server.MATE.domain.answer.dto.response.AnswerBatchCreateResponse;
import server.MATE.domain.answer.service.handler.AnswerCreateHandler;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnswerService {

    private final TestRepository testRepository;
    private final ParticipationRepository participationRepository;
    private final QuestionRepository questionRepository;
    private final Map<QuestionType, AnswerCreateHandler> handlerMap;

    public AnswerService(TestRepository testRepository,
                         ParticipationRepository participationRepository,
                         QuestionRepository questionRepository,
                         List<AnswerCreateHandler> handlers) {
        this.testRepository = testRepository;
        this.participationRepository = participationRepository;
        this.questionRepository = questionRepository;
        this.handlerMap = buildHandlerMap(handlers);
    }

    @Transactional
    public AnswerBatchCreateResponse createAnswers(Long testId, Long testerId, AnswerCreateRequest request) {
        Test test = testRepository.findByIdAndDeletedAtIsNullForUpdate(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        test.validateCanParticipate();

        if (participationRepository.existsByTestIdAndTesterIdAndDeletedAtIsNull(testId, testerId)) {
            throw new BaseException(BaseErrorCode.PARTICIPATION_003);
        }

        Map<Long, Question> questionMap = questionRepository
                .findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(testId)
                .stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        if (questionMap.size() != request.answers().size()) {
            throw new BaseException(BaseErrorCode.ANSWER_008);
        }

        Participation participation = Participation.builder()
                .testId(testId)
                .testerId(testerId)
                .build();
        participationRepository.save(participation);

        Set<Long> processedQuestionIds = new HashSet<>();
        for (AnswerCreateItem item : request.answers()) {
            if (!processedQuestionIds.add(item.questionId())) {
                throw new BaseException(BaseErrorCode.ANSWER_003);
            }

            Question question = questionMap.get(item.questionId());
            if (question == null) {
                throw new BaseException(BaseErrorCode.ANSWER_002);
            }

            if (question.getQuestionType() != item.type()) {
                throw new BaseException(BaseErrorCode.ANSWER_001);
            }

            AnswerCreateHandler handler = handlerMap.get(item.type());
            if (handler == null) {
                throw new BaseException(BaseErrorCode.COMMON_999);
            }

            handler.save(participation.getId(), item);
        }

        test.incrementPplCount();
        return AnswerBatchCreateResponse.from(participation);
    }

    private Map<QuestionType, AnswerCreateHandler> buildHandlerMap(List<AnswerCreateHandler> handlers) {
        Map<QuestionType, AnswerCreateHandler> map = new EnumMap<>(QuestionType.class);
        for (AnswerCreateHandler handler : handlers) {
            map.put(handler.supports(), handler);
        }
        return map;
    }
}
