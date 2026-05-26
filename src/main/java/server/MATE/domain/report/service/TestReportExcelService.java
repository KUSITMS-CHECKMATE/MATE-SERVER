package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TestReportExcelService {

    private final CombinedTestReportExcelService combinedTestReportExcelService;

    public TestReportExcelDownload export(Long testId, Long makerId) {
        return combinedTestReportExcelService.export(testId, makerId);
    }
}
