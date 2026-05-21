package server.MATE.domain.report.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.dto.response.QuestionReportItem;
import server.MATE.domain.report.dto.response.TestReportResponse;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final Map<QuestionType, ReportHandler> handlerMap;

    public ReportService(TestRepository testRepository,
                         QuestionRepository questionRepository,
                         AnswerRepository answerRepository,
                         List<ReportHandler> handlers) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.handlerMap = buildHandlerMap(handlers);
    }

    public TestReportResponse getReport(Long testId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (!test.getMakerId().equals(makerId)) throw new BaseException(BaseErrorCode.TEST_005);

        List<Question> questions = questionRepository.findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(testId);

        List<QuestionSummaryItem> questionSummaries = questions.stream()
                .map(q -> new QuestionSummaryItem(q.getId(), q.getSequence(), q.getTitle(), q.getQuestionType()))
                .toList();

        if (test.getTestStatus() == TestStatus.IN_PROGRESS) {
            return new TestReportResponse(
                    TestStatus.IN_PROGRESS,
                    questions.size(),
                    test.getPplCount(),
                    questionSummaries,
                    List.of()
            );
        }

        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        List<Answer> allAnswers = answerRepository.findAllByQuestionIdInAndDeletedAtIsNull(questionIds);

        Map<Long, List<Answer>> answersByQuestionId = allAnswers.stream()
                .collect(Collectors.groupingBy(Answer::getQuestionId));

        Map<QuestionType, List<Question>> questionsByType = questions.stream()
                .collect(Collectors.groupingBy(
                        Question::getQuestionType,
                        () -> new EnumMap<>(QuestionType.class),
                        Collectors.toList()
                ));

        Map<Long, Object> reportByQuestionId = new LinkedHashMap<>();
        for (Map.Entry<QuestionType, List<Question>> entry : questionsByType.entrySet()) {
            ReportHandler handler = handlerMap.get(entry.getKey());
            if (handler == null) throw new BaseException(BaseErrorCode.COMMON_002);
            reportByQuestionId.putAll(handler.compute(entry.getValue(), answersByQuestionId));
        }

        List<QuestionReportItem> results = questions.stream()
                .map(q -> new QuestionReportItem(
                        q.getId(),
                        q.getSequence(),
                        q.getTitle(),
                        q.getQuestionType(),
                        reportByQuestionId.get(q.getId())
                ))
                .toList();

        return new TestReportResponse(
                TestStatus.COMPLETED,
                questions.size(),
                test.getPplCount(),
                questionSummaries,
                results
        );
    }

    private Map<QuestionType, ReportHandler> buildHandlerMap(List<ReportHandler> handlers) {
        Map<QuestionType, ReportHandler> map = new EnumMap<>(QuestionType.class);
        for (ReportHandler handler : handlers) {
            map.put(handler.supports(), handler);
        }
        return map;
    }
}
