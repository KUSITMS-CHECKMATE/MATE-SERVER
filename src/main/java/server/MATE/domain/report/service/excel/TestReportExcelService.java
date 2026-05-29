package server.MATE.domain.report.service.excel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.service.excel.support.ReportExcelExportSupport;
import server.MATE.domain.test.entity.Test;
import server.MATE.global.storage.FileStorageService;

@Service
@RequiredArgsConstructor
public class TestReportExcelService {

    private final CombinedTestReportExcelService combinedTestReportExcelService;
    private final ReportExcelExportSupport reportExcelExportSupport;
    private final FileStorageService fileStorageService;

    @Transactional
    public TestReportExcelDownload export(Long testId, Long makerId) {
        Test test = reportExcelExportSupport.requireExportReadyTest(testId, makerId);

        if (test.getExcelKey() != null) {
            return new TestReportExcelDownload(
                    fileStorageService.generateDownloadUrl(test.getExcelKey()),
                    buildFilename(testId)
            );
        }

        byte[] excelBytes = combinedTestReportExcelService.export(testId, makerId);
        String excelKey = "reports/excel/" + testId + ".xlsx";
        fileStorageService.upload(excelKey, excelBytes, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        test.saveExcelKey(excelKey);

        return new TestReportExcelDownload(
                fileStorageService.generateDownloadUrl(excelKey),
                buildFilename(testId)
        );
    }

    private String buildFilename(Long testId) {
        return "mate-report-" + testId + ".xlsx";
    }
}
