package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.MateReportExcelWriter;
import server.MATE.domain.report.excel.TestReportExcelData;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TestReportExcelService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final MateReportExcelWriter mateReportExcelWriter;

    public TestReportExcelDownload export(Long testId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.TEST_005);
        }

        List<QuestionSummaryItem> questions = questionRepository.findQuestionSummariesByTestId(testId);
        if (questions.size() > MateReportExcelWriter.MAX_QUESTION_ROWS) {
            throw new BaseException(BaseErrorCode.REPORT_001);
        }

        TestReportExcelData data = new TestReportExcelData(
                test.getTitle(),
                test.getDescription(),
                formatTestPeriod(test),
                test.getGoalPpl(),
                questions
        );

        byte[] content = mateReportExcelWriter.write(data);
        return new TestReportExcelDownload(content, buildFilename(testId));
    }

    private String formatTestPeriod(Test test) {
        if (test.getCreatedAt() == null) {
            return "";
        }

        String start = DATE_FORMAT.format(test.getCreatedAt());
        if (test.getTestStatus() == TestStatus.COMPLETED && test.getUpdatedAt() != null) {
            return start + " ~ " + DATE_FORMAT.format(test.getUpdatedAt());
        }
        return start + " ~ 진행 중";
    }

    private String buildFilename(Long testId) {
        return "mate-report-" + testId + ".xlsx";
    }
}
