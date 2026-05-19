package server.MATE.domain.question.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.request.QuestionCreateItem;
import server.MATE.domain.question.dto.request.QuestionCreateRequest;
import server.MATE.domain.question.dto.response.QuestionCreateResponse;
import server.MATE.domain.question.dto.response.QuestionCreateResult;
import server.MATE.domain.question.dto.response.QuestionDetailItem;
import server.MATE.domain.question.dto.response.QuestionDetailResponse;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.dto.response.QuestionSummaryResponse;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.service.handler.QuestionCreateHandler;
import server.MATE.domain.question.service.fetcher.QuestionDetailFetcher;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.storage.event.FileCleanupEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuestionService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<QuestionType, QuestionCreateHandler> handlerMap;
    private final Map<QuestionType, QuestionDetailFetcher> fetcherMap;

    public QuestionService(TestRepository testRepository,
                           QuestionRepository questionRepository,
                           ApplicationEventPublisher eventPublisher,
                           List<QuestionCreateHandler> handlers,
                           List<QuestionDetailFetcher> fetchers) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.eventPublisher = eventPublisher;
        this.handlerMap = buildHandlerMap(handlers);
        this.fetcherMap = buildFetcherMap(fetchers);
    }

    public QuestionCreateResponse createQuestions(Long testId, Long makerId, QuestionCreateRequest request) {

        // 테스트 내 질문의 시퀀스 할당 시 경쟁을 방지하기 위해 부모 행을 잠금
        // 이를 통해 MAX(sequence) 조회 및 생성 요청을 직렬화하여 처리함
        Test test = testRepository.findByIdAndDeletedAtIsNullForUpdate(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (!test.getMakerId().equals(makerId)) throw new BaseException(BaseErrorCode.TEST_005);

        Long baseSequence = questionRepository.findMaxSequenceByTestId(testId);

        List<String> imageKeysToCleanup = new ArrayList<>();
        List<QuestionCreateResult> results = new ArrayList<>();
        List<PendingQuestionCreate> pendingCreates = new ArrayList<>();

        List<QuestionCreateItem> items = request.questions();
        for (int i = 0; i < items.size(); i++) {
            QuestionCreateItem item = items.get(i);
            QuestionCreateHandler handler = handlerMap.get(item.type());
            if (handler == null) throw new BaseException(BaseErrorCode.COMMON_002);

            handler.validate(item);
            imageKeysToCleanup.addAll(handler.extractImageKeys(item));
            pendingCreates.add(new PendingQuestionCreate(handler, item, baseSequence + i + 1));
        }

        if (!imageKeysToCleanup.isEmpty()) eventPublisher.publishEvent(new FileCleanupEvent(imageKeysToCleanup));

        for (PendingQuestionCreate pendingCreate : pendingCreates) {
            QuestionCreateItem item = pendingCreate.item();
            Question question = Question.builder()
                    .testId(testId)
                    .questionType(item.type())
                    .title(item.title())
                    .description(item.description())
                    .sequence(pendingCreate.sequence())
                    .build();
            questionRepository.save(question);

            pendingCreate.handler().createDetail(question, item);
            results.add(QuestionCreateResult.from(question));
        }

        return new QuestionCreateResponse(results);
    }

    @Transactional(readOnly = true)
    public QuestionDetailResponse getQuestions(Long testId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (!test.getMakerId().equals(makerId)) throw new BaseException(BaseErrorCode.TEST_005);

        List<Question> questions = questionRepository.findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(testId);
        Map<QuestionType, List<Question>> questionsByType = questions.stream()
                .collect(Collectors.groupingBy(
                        Question::getQuestionType,
                        () -> new EnumMap<>(QuestionType.class),
                        Collectors.toList()
                ));

        Map<Long, QuestionDetailItem> detailMap = new LinkedHashMap<>();
        for (Map.Entry<QuestionType, List<Question>> entry : questionsByType.entrySet()) {
            QuestionDetailFetcher fetcher = fetcherMap.get(entry.getKey());
            if (fetcher == null) throw new BaseException(BaseErrorCode.COMMON_002);
            detailMap.putAll(fetcher.fetch(entry.getValue()));
        }

        List<QuestionDetailItem> results = questions.stream()
                .map(question -> detailMap.get(question.getId()))
                .toList();

        return new QuestionDetailResponse(testId, results);
    }

    @Transactional(readOnly = true)
    public QuestionSummaryResponse getQuestionSummary(Long testId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (!test.getMakerId().equals(makerId)) throw new BaseException(BaseErrorCode.TEST_005);
        if (test.getTestStatus() != TestStatus.COMPLETED) throw new BaseException(BaseErrorCode.TEST_006);

        List<QuestionSummaryItem> questions = questionRepository.findQuestionSummariesByTestId(testId);
        return new QuestionSummaryResponse(
                questions.size(),
                test.getPplCount(),
                questions
        );
    }

    private Map<QuestionType, QuestionCreateHandler> buildHandlerMap(List<QuestionCreateHandler> handlers) {
        Map<QuestionType, QuestionCreateHandler> handlerMap = new EnumMap<>(QuestionType.class);
        for (QuestionCreateHandler handler : handlers) {
            handlerMap.put(handler.supports(), handler);
        }
        return handlerMap;
    }

    private Map<QuestionType, QuestionDetailFetcher> buildFetcherMap(List<QuestionDetailFetcher> fetchers) {
        Map<QuestionType, QuestionDetailFetcher> fetcherMap = new EnumMap<>(QuestionType.class);
        for (QuestionDetailFetcher fetcher : fetchers) {
            fetcherMap.put(fetcher.supports(), fetcher);
        }
        return fetcherMap;
    }

    private record PendingQuestionCreate(
            QuestionCreateHandler handler,
            QuestionCreateItem item,
            Long sequence
    ) {
    }
}
