package server.MATE.domain.answer.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.AnswerCreateRequest;
import server.MATE.domain.answer.dto.response.AnswerBatchCreateResponse;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.answer.service.handler.AnswerCreateHandler;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.question.entity.*;
import server.MATE.domain.test.event.TestCompletedEvent;
import server.MATE.domain.question.repository.*;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnswerService {

    private final TestRepository testRepository;
    private final ParticipationRepository participationRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ObjectiveRepository objectiveRepository;
    private final FiveSecondRepository fiveSecondRepository;
    private final ScaleRepository scaleRepository;
    private final CardSortingRepository cardSortingRepository;
    private final TreeTestRepository treeTestRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<QuestionType, AnswerCreateHandler> handlerMap;

    public AnswerService(TestRepository testRepository,
                         ParticipationRepository participationRepository,
                         QuestionRepository questionRepository,
                         AnswerRepository answerRepository,
                         ObjectiveRepository objectiveRepository,
                         FiveSecondRepository fiveSecondRepository,
                         ScaleRepository scaleRepository,
                         CardSortingRepository cardSortingRepository,
                         TreeTestRepository treeTestRepository,
                         ApplicationEventPublisher eventPublisher,
                         List<AnswerCreateHandler> handlers) {
        this.testRepository = testRepository;
        this.participationRepository = participationRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.objectiveRepository = objectiveRepository;
        this.fiveSecondRepository = fiveSecondRepository;
        this.scaleRepository = scaleRepository;
        this.cardSortingRepository = cardSortingRepository;
        this.treeTestRepository = treeTestRepository;
        this.eventPublisher = eventPublisher;
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

            if (!handlerMap.containsKey(item.type())) {
                throw new BaseException(BaseErrorCode.COMMON_999);
            }
        }

        AnswerCreateContext context = buildContext(request.answers());

        List<Answer> answers = new ArrayList<>();
        for (AnswerCreateItem item : request.answers()) {
            answers.add(handlerMap.get(item.type()).build(participation.getId(), item, context));
        }

        answerRepository.saveAll(answers);
        test.incrementPplCount();
        if (test.getPplCount() >= test.getGoalPpl()) {
            test.complete();
            test.startReportAggregation();
            eventPublisher.publishEvent(new TestCompletedEvent(testId));
        }
        return AnswerBatchCreateResponse.from(participation);
    }

    private AnswerCreateContext buildContext(List<AnswerCreateItem> items) {
        Map<QuestionType, List<Long>> idsByType = new EnumMap<>(QuestionType.class);
        for (AnswerCreateItem item : items) {
            idsByType.computeIfAbsent(item.type(), k -> new ArrayList<>()).add(item.questionId());
        }

        Map<Long, Objective> objectives = fetchByType(idsByType, QuestionType.OBJECTIVE,
                ids -> objectiveRepository.findAllByIdIn(ids), Objective::getId);

        Map<Long, FiveSecond> fiveSeconds = fetchByType(idsByType, QuestionType.FIVE_SECOND,
                ids -> fiveSecondRepository.findAllByIdIn(ids), FiveSecond::getId);

        Map<Long, Scale> scales = fetchByType(idsByType, QuestionType.SCALE,
                ids -> scaleRepository.findAllByIdIn(ids), Scale::getId);

        Map<Long, CardSorting> cardSortings = fetchByType(idsByType, QuestionType.CARD_SORTING,
                ids -> cardSortingRepository.findAllByIdIn(ids), CardSorting::getId);

        List<Long> treeTestIds = idsByType.getOrDefault(QuestionType.TREE_TEST, List.of());
        Map<Long, List<TreeTest>> treeNodes = treeTestIds.isEmpty()
                ? Collections.emptyMap()
                : treeTestRepository.findAllByQuestionIdInOrderByQuestionAndTree(treeTestIds)
                        .stream()
                        .collect(Collectors.groupingBy(node -> node.getQuestion().getId()));

        return new AnswerCreateContext(objectives, fiveSeconds, scales, cardSortings, treeNodes);
    }

    private <E> Map<Long, E> fetchByType(Map<QuestionType, List<Long>> idsByType,
                                          QuestionType type,
                                          java.util.function.Function<List<Long>, List<E>> fetcher,
                                          java.util.function.Function<E, Long> idExtractor) {
        List<Long> ids = idsByType.get(type);
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        return fetcher.apply(ids).stream().collect(Collectors.toMap(idExtractor, e -> e));
    }

    private Map<QuestionType, AnswerCreateHandler> buildHandlerMap(List<AnswerCreateHandler> handlers) {
        Map<QuestionType, AnswerCreateHandler> map = new EnumMap<>(QuestionType.class);
        for (AnswerCreateHandler handler : handlers) {
            map.put(handler.supports(), handler);
        }
        return map;
    }
}
