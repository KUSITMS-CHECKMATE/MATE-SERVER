package server.MATE.domain.report.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.dto.response.ReportItem;
import server.MATE.domain.report.dto.response.ReportResponse;
import server.MATE.domain.report.entity.Report;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final ReportRepository reportRepository;

    public ReportResponse getReport(Long testId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (!test.getMakerId().equals(makerId)) throw new BaseException(BaseErrorCode.TEST_005);

        List<Question> questions = questionRepository.findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(testId);
        List<QuestionSummaryItem> questionSummaries = questions.stream()
                .map(q -> new QuestionSummaryItem(q.getId(), q.getSequence(), q.getTitle(), q.getQuestionType()))
                .toList();

        // 테스트가 진행 중이고, 리포트 집계가 시작되지 않은 상태. reports를 빈 리스트로 반환
        if (test.getTestStatus() == TestStatus.IN_PROGRESS) {
            return new ReportResponse(
                    TestStatus.IN_PROGRESS,
                    ReportStatus.PENDING,
                    questions.size(),
                    test.getPplCount(),
                    questionSummaries,
                    List.of()
            );
        }

        // 테스트가 완료이지만, 리포트 집계가 끝나지 않음. reportStatus를 업데이트하고 reports를 빈 리스트로 반환
        ReportStatus reportStatus = test.getReportStatus() != null ? test.getReportStatus() : ReportStatus.PENDING;
        if (reportStatus != ReportStatus.COMPLETED) {
            return new ReportResponse(
                    TestStatus.COMPLETED,
                    reportStatus,
                    questions.size(),
                    test.getPplCount(),
                    questionSummaries,
                    List.of()
            );
        }

        // 테스트가 완료이고, 리포트 집계가 끝난 상태. 질문별 집계 결과를 반환
        List<Report> aggregations = reportRepository.findAllByTestId(testId);

        if (aggregations.size() != questions.size()) {
            log.error("테스트 {} 리포트 조회 중 완전성 불일치 감지: questionCount={}, reportCount={}",
                    testId, questions.size(), aggregations.size());
            return new ReportResponse(
                    TestStatus.COMPLETED,
                    ReportStatus.FAILED,
                    questions.size(),
                    test.getPplCount(),
                    questionSummaries,
                    List.of()
            );
        }

        Map<Long, Map<String, Object>> resultByQuestionId = aggregations.stream()
                .collect(Collectors.toMap(Report::getQuestionId, Report::getResult));

        List<ReportItem> reports = questions.stream()
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
                ReportStatus.COMPLETED,
                questions.size(),
                test.getPplCount(),
                questionSummaries,
                reports
        );
    }
}
