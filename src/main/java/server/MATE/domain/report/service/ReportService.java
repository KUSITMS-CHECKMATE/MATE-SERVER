package server.MATE.domain.report.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
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

        int questionCount = Math.toIntExact(questionRepository.countByTestIdAndDeletedAtIsNull(testId));
        ReportStatus reportStatus = test.getReportStatus() != null ? test.getReportStatus() : ReportStatus.PENDING;

        switch (test.getTestStatus()) {
            case WAITING, IN_PROGRESS, REJECTED -> {
                // 완료 전 상태이므로 집계 결과 없이 현재 리포트 상태만 반환함
                return new ReportResponse(
                        test.getTestStatus(),
                        reportStatus,
                        questionCount,
                        test.getPplCount(),
                        List.of()
                );
            }
            case COMPLETED -> {
                // 테스트는 종료됐지만 집계가 끝나지 않았으면 빈 결과를 반환함
                if (reportStatus != ReportStatus.COMPLETED) {
                    return new ReportResponse(
                            TestStatus.COMPLETED,
                            reportStatus,
                            questionCount,
                            test.getPplCount(),
                            List.of()
                    );
                }
            }
        }

        // 테스트가 종료됐고 리포트 집계가 끝났다면 리포트를 반환함
        List<Report> aggregations = reportRepository.findAllByTestId(testId);

        if (aggregations.size() != questionCount) {
            log.error("테스트 {} 리포트 조회 중 완전성 불일치 감지: questionCount={}, reportCount={}",
                    testId, questionCount, aggregations.size());
            return new ReportResponse(
                    test.getTestStatus(),
                    ReportStatus.FAILED,
                    questionCount,
                    test.getPplCount(),
                    List.of()
            );
        }

        Map<Long, Map<String, Object>> resultByQuestionId = aggregations.stream()
                .collect(Collectors.toMap(Report::getQuestionId, Report::getResult));

        List<QuestionSummaryItem> questionSummaries = questionRepository.findQuestionSummariesByTestId(testId);

        // 질문 요약 projection을 reports 조립에 재사용
        List<ReportItem> reports = questionSummaries.stream()
                .map(question -> new ReportItem(
                        question.questionId(),
                        question.sequence(),
                        question.title(),
                        question.type(),
                        resultByQuestionId.get(question.questionId())
                ))
                .toList();

        return new ReportResponse(
                TestStatus.COMPLETED,
                ReportStatus.COMPLETED,
                questionCount,
                test.getPplCount(),
                reports
        );
    }
}
