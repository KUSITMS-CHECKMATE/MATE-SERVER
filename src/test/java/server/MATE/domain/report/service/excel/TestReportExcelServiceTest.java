package server.MATE.domain.report.service.excel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.service.excel.support.ReportExcelExportSupport;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.storage.FileStorageService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestReportExcelServiceTest {

    private static final Long TEST_ID = 10L;
    private static final Long MAKER_ID = 1L;

    @Mock
    private CombinedTestReportExcelService combinedTestReportExcelService;
    @Mock
    private ReportExcelExportSupport reportExcelExportSupport;
    @Mock
    private FileStorageService fileStorageService;

    private TestReportExcelService testReportExcelService;

    @BeforeEach
    void setUp() {
        testReportExcelService = new TestReportExcelService(
                combinedTestReportExcelService,
                reportExcelExportSupport,
                fileStorageService
        );
    }

    @Test
    void excelKey가_없으면_생성_후_업로드하고_바이너리를_반환한다() {
        server.MATE.domain.test.entity.Test test = completedTest(null);
        byte[] excelBytes = {1, 2, 3};
        given(reportExcelExportSupport.requireExportReadyTest(TEST_ID, MAKER_ID)).willReturn(test);
        given(combinedTestReportExcelService.export(TEST_ID, MAKER_ID)).willReturn(excelBytes);

        TestReportExcelDownload result = testReportExcelService.export(TEST_ID, MAKER_ID);

        assertThat(result.data()).isEqualTo(excelBytes);
        assertThat(result.filename()).isEqualTo("mate-report-" + TEST_ID + ".xlsx");
        verify(fileStorageService).upload(
                eq("reports/excel/" + TEST_ID + ".xlsx"),
                eq(excelBytes),
                eq("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        );
        verify(reportExcelExportSupport).saveExcelKey(TEST_ID, "reports/excel/" + TEST_ID + ".xlsx");
    }

    @Test
    void excelKey가_있으면_재생성_없이_저장된_파일을_바이너리로_반환한다() {
        String excelKey = "reports/excel/" + TEST_ID + ".xlsx";
        server.MATE.domain.test.entity.Test test = completedTest(excelKey);
        byte[] cachedBytes = {4, 5, 6};
        given(reportExcelExportSupport.requireExportReadyTest(TEST_ID, MAKER_ID)).willReturn(test);
        given(fileStorageService.download(excelKey)).willReturn(cachedBytes);

        TestReportExcelDownload result = testReportExcelService.export(TEST_ID, MAKER_ID);

        assertThat(result.data()).isEqualTo(cachedBytes);
        assertThat(result.filename()).isEqualTo("mate-report-" + TEST_ID + ".xlsx");
        verify(combinedTestReportExcelService, never()).export(any(), any());
        verify(fileStorageService, never()).upload(any(), any(byte[].class), any());
    }

    private server.MATE.domain.test.entity.Test completedTest(String excelKey) {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(MAKER_ID)
                .title("테스트")
                .description("설명")
                .goalPpl(10)
                .testStatus(TestStatus.COMPLETED)
                .closedAt(LocalDateTime.of(2026, 6, 1, 23, 59, 59))
                .build();
        ReflectionTestUtils.setField(test, "id", TEST_ID);
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.COMPLETED);
        if (excelKey != null) {
            test.saveExcelKey(excelKey);
        }
        return test;
    }
}
