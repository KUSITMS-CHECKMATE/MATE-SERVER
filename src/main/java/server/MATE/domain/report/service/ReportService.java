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
import server.MATE.domain.users.entity.Role;
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

    @Transactional
    public ReportResponse getReport(Long testId, Long userId, Role role) {
        Test test = testRepository.findActiveById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (role != Role.ADMIN && !test.getMakerId().equals(userId)) throw new BaseException(BaseErrorCode.TEST_005);

        int questionCount = Math.toIntExact(questionRepository.countQuestionsInTest(testId));
        ReportStatus reportStatus = test.getReportStatus() != null ? test.getReportStatus() : ReportStatus.PENDING;

        switch (test.getTestStatus()) {
            case WAITING, IN_PROGRESS, REJECTED -> {
                // 완료 전 상태이므로 집계 결과 없이 현재 리포트 상태만 반환함
                return new ReportResponse(
                        test.getTitle(),
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
                            test.getTitle(),
                            test.getTestStatus(),
                            reportStatus,
                            questionCount,
                            test.getPplCount(),
                            List.of()
                    );
                }
            }
        }

        if (test.getMakerId().equals(userId)) {
            markResultViewedIfNeeded(test);
        }

        List<Report> aggregations = reportRepository.findAllByTestId(testId);
        List<QuestionSummaryItem> questionSummaries = questionRepository.findQuestionSummariesInTest(testId);

        Map<Long, Map<String, Object>> resultByQuestionId = aggregations.stream()
                .collect(Collectors.toMap(Report::getQuestionId, Report::getResult));

        List<Long> missingQuestionIds = questionSummaries.stream()
                .map(QuestionSummaryItem::questionId)
                .filter(questionId -> !resultByQuestionId.containsKey(questionId))
                .toList();

        if (!missingQuestionIds.isEmpty()) {
            log.error("테스트 {} 리포트 조회 중 활성 질문 리포트 누락 감지: questionCount={}, reportCount={}, missingQuestionIds={}",
                    testId, questionCount, aggregations.size(), missingQuestionIds);
            return new ReportResponse(
                    test.getTitle(),
                    test.getTestStatus(),
                    ReportStatus.FAILED,
                    questionCount,
                    test.getPplCount(),
                    List.of()
            );
        }

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
                test.getTitle(),
                TestStatus.COMPLETED,
                ReportStatus.COMPLETED,
                questionCount,
                test.getPplCount(),
                reports
        );
    }

    private void markResultViewedIfNeeded(Test test) {
        if (test.getResultViewedAt() == null) {
            test.markResultViewed();
        }
    }
}
