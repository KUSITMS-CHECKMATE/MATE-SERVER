package server.MATE.domain.report.event;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.entity.Report;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.report.service.ReportHandler;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Slf4j
@Service
public class ReportAggregateService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ReportRepository reportRepository;
    private final TestRepository testRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<QuestionType, ReportHandler> handlerMap;

    public ReportAggregateService(QuestionRepository questionRepository,
                                  AnswerRepository answerRepository,
                                  ReportRepository reportRepository,
                                  TestRepository testRepository,
                                  ApplicationEventPublisher eventPublisher,
                                  List<ReportHandler> handlers) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.reportRepository = reportRepository;
        this.testRepository = testRepository;
        this.eventPublisher = eventPublisher;
        this.handlerMap = buildHandlerMap(handlers);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2),
            noRetryFor = {BaseException.class, DataIntegrityViolationException.class})
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Report> aggregate(Long testId) {
        // 존재 검증 + 실패 분기(failReportAggregation)에만 쓰는 스냅샷. 완료 확정은 markCompletedAndNotify가
        // 별도로 락을 잡고 다시 조회해 처리하므로, 이 test로 완료 상태를 직접 저장하지 않는다.
        Test test = testRepository.findActiveById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        long questionCount = questionRepository.countQuestionsInTest(testId);
        long reportCount = reportRepository.countByTestId(testId);

        // 질문 수와 리포트 수가 같으면 이미 전체 집계가 끝난 상태로 처리
        // 질문과 리포트가 모두 0개인 경우를 포함함
        if (reportCount == questionCount) {
            markCompletedAndNotify(testId);
            return reportRepository.findAllByTestId(testId);
        }

        // 일부 리포트만 남아 있으면 불일치 상태이므로 실패 처리
        if (reportCount > 0) {
            log.error("테스트 {} 리포트 완전성 불일치 감지: questionCount={}, reportCount={}",
                    testId, questionCount, reportCount);
            test.failReportAggregation();
            testRepository.save(test);
            return List.of();
        }

        long startedAt = System.currentTimeMillis();
        List<Question> questions = questionRepository.findQuestionsInTest(testId);
        Map<Long, Map<String, Object>> resultByQuestionId = computeResultByQuestionId(questions, true);

        List<Report> reports = questions.stream()
                .map(q -> Report.builder()
                        .testId(testId)
                        .questionId(q.getId())
                        .questionType(q.getQuestionType())
                        .result(resultByQuestionId.get(q.getId()))
                        .build())
                .toList();

        List<Report> saved = reportRepository.saveAll(reports);
        log.info("테스트 {} 리포트 집계 완료: 문항 {}개, 소요시간 {}ms",
                testId, questions.size(), System.currentTimeMillis() - startedAt);
        markCompletedAndNotify(testId);
        return saved;
    }

    /**
     * 테스트 진행 중(50% 이상 응답 시점)에 결과 탭에서 호출되는 즉석 집계.
     * Report 테이블에 저장하지 않고, 주관식 계열 핸들러는 AI 분석(Claude 호출) 없이 raw 텍스트만 반환함.
     */
    @Transactional(readOnly = true)
    public Map<Long, Map<String, Object>> computeLive(Long testId) {
        List<Question> questions = questionRepository.findQuestionsInTest(testId);
        return computeResultByQuestionId(questions, false);
    }

    private Map<Long, Map<String, Object>> computeResultByQuestionId(List<Question> questions, boolean includeAiAnalysis) {
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        List<Answer> allAnswers = answerRepository.findAllByQuestionIdInAndDeletedAtIsNull(questionIds);

        // 질문별 집계 계산에 바로 쓸 수 있도록 응답을 questionId 기준으로 묶음
        Map<Long, List<Answer>> answersByQuestionId = allAnswers.stream()
                .collect(Collectors.groupingBy(Answer::getQuestionId));
        Map<QuestionType, List<Question>> questionsByType = questions.stream()
                .collect(Collectors.groupingBy(
                        Question::getQuestionType,
                        () -> new EnumMap<>(QuestionType.class),
                        Collectors.toList()
                ));

        // 질문 유형별 핸들러로 부분 집계를 수행, questionId 기준으로 결과 맵 저장
        Map<Long, Map<String, Object>> resultByQuestionId = new LinkedHashMap<>();
        for (Map.Entry<QuestionType, List<Question>> entry : questionsByType.entrySet()) {
            ReportHandler handler = handlerMap.get(entry.getKey());
            if (handler == null) throw new BaseException(BaseErrorCode.COMMON_002);
            resultByQuestionId.putAll(handler.compute(entry.getValue(), answersByQuestionId, includeAiAnalysis));
        }
        return resultByQuestionId;
    }

    @Recover
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Report> recover(Exception e, Long testId) {
        long questionCount = questionRepository.countQuestionsInTest(testId);
        long reportCount = reportRepository.countByTestId(testId);

        if (e instanceof DataIntegrityViolationException || reportCount > 0) {
            if (reportCount == questionCount) {
                log.info("테스트 {} 리포트가 이미 완전하게 존재합니다. 상태를 완료로 업데이트합니다.", testId);
                markCompletedAndNotify(testId);
                return reportRepository.findAllByTestId(testId);
            }

            log.error("테스트 {} recover 중 리포트 완전성 불일치 감지: questionCount={}, reportCount={}",
                    testId, questionCount, reportCount, e);
            updateReportStatus(testId, testRepository::findActiveById, Test::failReportAggregation);
            return List.of();
        }

        log.error("테스트 {} 집계 3회 실패", testId, e);
        updateReportStatus(testId, testRepository::findActiveById, Test::failReportAggregation);
        return List.of();
    }

    private void updateReportStatus(Long testId, Function<Long, Optional<Test>> fetcher, Consumer<Test> action) {
        fetcher.apply(testId).ifPresentOrElse(
                test -> {
                    action.accept(test);
                    testRepository.save(test);
                },
                () -> log.warn("테스트 {} 상태 업데이트 대상을 찾을 수 없습니다. 삭제되었을 수 있습니다.", testId)
        );
    }

    // 완료 확정 직전에만 짧게 락을 잡아 aggregate()/recover()의 동시 실행을 직렬화한다.
    // AI 분석 등 느린 연산 중에는 락을 잡지 않아, 같은 테스트에 대한 응답 제출(AnswerService)과 경합하지 않는다.
    private void markCompletedAndNotify(Long testId) {
        updateReportStatus(testId, testRepository::findByIdForUpdate, this::completeAndPublishIfFirstTime);
    }

    private void completeAndPublishIfFirstTime(Test test) {
        boolean alreadyCompleted = test.getReportStatus() == ReportStatus.COMPLETED;
        test.completeReportAggregation();
        if (!alreadyCompleted) {
            eventPublisher.publishEvent(new ReportCompletedEvent(test.getId(), test.getMakerId(), test.getTitle()));
        }
    }

    private Map<QuestionType, ReportHandler> buildHandlerMap(List<ReportHandler> handlers) {
        Map<QuestionType, ReportHandler> map = new EnumMap<>(QuestionType.class);
        for (ReportHandler handler : handlers) {
            map.put(handler.supports(), handler);
        }
        return map;
    }
}
