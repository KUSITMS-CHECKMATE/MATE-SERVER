package server.MATE.domain.report.event;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
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
    private final Map<QuestionType, ReportHandler> handlerMap;

    public ReportAggregateService(QuestionRepository questionRepository,
                                  AnswerRepository answerRepository,
                                  ReportRepository reportRepository,
                                  TestRepository testRepository,
                                  List<ReportHandler> handlers) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.reportRepository = reportRepository;
        this.testRepository = testRepository;
        this.handlerMap = buildHandlerMap(handlers);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2),
            noRetryFor = {BaseException.class, DataIntegrityViolationException.class})
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Report> aggregate(Long testId) {
        Test test = testRepository.findActiveById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        long questionCount = questionRepository.countByTestIdAndDeletedAtIsNull(testId);
        long reportCount = reportRepository.countByTestId(testId);

        // 질문 수와 리포트 수가 같으면 이미 전체 집계가 끝난 상태로 처리
        // 질문과 리포트가 모두 0개인 경우를 포함함
        if (reportCount == questionCount) {
            test.completeReportAggregation();
            testRepository.save(test);
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

        List<Question> questions = questionRepository.findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(testId);
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

        // 질문 유형별 핸들러로 부분 집계를 수행, questionId 기준으로 결과 맵 저
        Map<Long, Map<String, Object>> resultByQuestionId = new LinkedHashMap<>();
        for (Map.Entry<QuestionType, List<Question>> entry : questionsByType.entrySet()) {
            ReportHandler handler = handlerMap.get(entry.getKey());
            if (handler == null) throw new BaseException(BaseErrorCode.COMMON_002);
            resultByQuestionId.putAll(handler.compute(entry.getValue(), answersByQuestionId));
        }

        List<Report> reports = questions.stream()
                .map(q -> Report.builder()
                        .testId(testId)
                        .questionId(q.getId())
                        .questionType(q.getQuestionType())
                        .result(resultByQuestionId.get(q.getId()))
                        .build())
                .toList();

        List<Report> saved = reportRepository.saveAll(reports);
        test.completeReportAggregation();
        testRepository.save(test);
        return saved;
    }

    @Recover
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Report> recover(Exception e, Long testId) {
        long questionCount = questionRepository.countByTestIdAndDeletedAtIsNull(testId);
        long reportCount = reportRepository.countByTestId(testId);

        if (e instanceof DataIntegrityViolationException || reportCount > 0) {
            if (reportCount == questionCount) {
                log.info("테스트 {} 리포트가 이미 완전하게 존재합니다. 상태를 완료로 업데이트합니다.", testId);
                updateReportStatus(testId, Test::completeReportAggregation);
                return reportRepository.findAllByTestId(testId);
            }

            log.error("테스트 {} recover 중 리포트 완전성 불일치 감지: questionCount={}, reportCount={}",
                    testId, questionCount, reportCount, e);
            updateReportStatus(testId, Test::failReportAggregation);
            return List.of();
        }

        log.error("테스트 {} 집계 3회 실패", testId, e);
        updateReportStatus(testId, Test::failReportAggregation);
        return List.of();
    }

    private void updateReportStatus(Long testId, Consumer<Test> action) {
        testRepository.findActiveById(testId).ifPresent(test -> {
            action.accept(test);
            testRepository.save(test);
        });
    }

    private Map<QuestionType, ReportHandler> buildHandlerMap(List<ReportHandler> handlers) {
        Map<QuestionType, ReportHandler> map = new EnumMap<>(QuestionType.class);
        for (ReportHandler handler : handlers) {
            map.put(handler.supports(), handler);
        }
        return map;
    }
}
