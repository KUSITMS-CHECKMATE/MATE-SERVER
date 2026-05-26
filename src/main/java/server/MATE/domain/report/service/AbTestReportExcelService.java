package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.AbTestReportExcelData;
import server.MATE.domain.report.excel.AbTestReportExcelWriter;
import server.MATE.global.common.exception.BaseErrorCode;

import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AbTestReportExcelService {

    private final ReportExcelExportSupport reportExcelExportSupport;
    private final AbTestReportExcelWriter abTestReportExcelWriter;

    public TestReportExcelDownload export(Long testId, Long questionId, Long makerId) {
        AbTestReportExcelData data = prepareData(testId, questionId, makerId);
        byte[] content = abTestReportExcelWriter.write(data);
        return new TestReportExcelDownload(
                content,
                buildFilename(testId, Long.parseLong(data.questionNumberLabel().substring(1)))
        );
    }

    public AbTestReportExcelData prepareData(Long testId, Long questionId, Long makerId) {
        reportExcelExportSupport.requireExportReadyTest(testId, makerId);
        Question question = reportExcelExportSupport.requireQuestion(
                testId, questionId, QuestionType.AB_TEST, BaseErrorCode.REPORT_004
        );
        Map<String, Object> reportResult = reportExcelExportSupport.requireReportResult(testId, questionId);
        ReportExcelResultMapper.AbTestCounts counts = ReportExcelResultMapper.toAbTestCounts(reportResult);

        return new AbTestReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                counts.versionACount() + counts.versionBCount(),
                counts.versionACount(),
                counts.versionBCount()
        );
    }

    private String buildFilename(Long testId, Long sequence) {
        return "mate-abtest-report-" + testId + "-q" + String.format("%02d", sequence) + ".xlsx";
    }
}
