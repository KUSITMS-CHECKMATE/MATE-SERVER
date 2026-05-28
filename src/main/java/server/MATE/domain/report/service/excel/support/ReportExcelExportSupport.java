package server.MATE.domain.report.service.excel.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Component
@RequiredArgsConstructor
public class ReportExcelExportSupport {

    private final TestRepository testRepository;

    public Test requireExportReadyTest(Long testId, Long makerId) {
        Test test = testRepository.findActiveById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.TEST_005);
        }
        if (test.getTestStatus() != TestStatus.COMPLETED) {
            throw new BaseException(BaseErrorCode.TEST_006);
        }

        ReportStatus reportStatus = test.getReportStatus() != null ? test.getReportStatus() : ReportStatus.PENDING;
        if (reportStatus != ReportStatus.COMPLETED) {
            throw new BaseException(BaseErrorCode.REPORT_007);
        }
        return test;
    }
}
