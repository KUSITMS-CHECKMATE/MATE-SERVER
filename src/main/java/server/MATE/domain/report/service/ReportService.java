package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.dto.response.ReportItem;
import server.MATE.domain.report.dto.response.ReportResponse;
import server.MATE.domain.report.entity.Report;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final ReportRepository reportRepository;
    private final ReportAggregationService reportAggregationService;

    public ReportResponse getReport(Long testId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (!test.getMakerId().equals(makerId)) throw new BaseException(BaseErrorCode.TEST_005);

        List<Question> questions = questionRepository.findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(testId);

        List<QuestionSummaryItem> questionSummaries = questions.stream()
                .map(q -> new QuestionSummaryItem(q.getId(), q.getSequence(), q.getTitle(), q.getQuestionType()))
                .toList();

        if (test.getTestStatus() == TestStatus.IN_PROGRESS) {
            return new ReportResponse(
                    TestStatus.IN_PROGRESS,
                    questions.size(),
                    test.getPplCount(),
                    questionSummaries,
                    List.of()  // stats
            );
        }

        List<Report> reports = reportRepository.findAllByTestId(testId);
        if (reports.isEmpty()) {
            reports = reportAggregationService.aggregate(testId);
        }

        Map<Long, Map<String, Object>> resultByQuestionId = reports.stream()
                .collect(Collectors.toMap(Report::getQuestionId, Report::getResult));

        List<ReportItem> stats = questions.stream()
                .map(q -> new ReportItem(
                        q.getId(),
                        q.getSequence(),
                        q.getTitle(),
                        q.getQuestionType(),
                        resultByQuestionId.get(q.getId())
                ))
                .toList();

        return new ReportResponse(
                TestStatus.COMPLETED,
                questions.size(),
                test.getPplCount(),
                questionSummaries,
                stats
        );
    }
}
