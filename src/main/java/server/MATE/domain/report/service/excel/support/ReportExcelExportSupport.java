package server.MATE.domain.report.service.excel.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Slf4j
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

    @Transactional
    public void savePdfKey(Long testId, String pdfKey) {
        int updated = testRepository.updatePdfKeyIfReportCompleted(testId, pdfKey);
        if (updated == 0) {
            log.warn("테스트 {} PDF 키 저장 생략 - 리포트 완료 상태가 아님(재개 등)", testId);
        }
    }

    @Transactional
    public void saveExcelKey(Long testId, String excelKey) {
        int updated = testRepository.updateExcelKeyIfReportCompleted(testId, excelKey);
        if (updated == 0) {
            log.warn("테스트 {} 엑셀 키 저장 생략 - 리포트 완료 상태가 아님(재개 등)", testId);
        }
    }
}
