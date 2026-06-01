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

    @org.junit.jupiter.api.Test
    void excelKey가_없으면_생성_후_업로드하고_URL을_반환한다() {
        server.MATE.domain.test.entity.Test test = completedTest(null);
        String filename = "mate-report-" + TEST_ID + ".xlsx";
        given(reportExcelExportSupport.requireExportReadyTest(TEST_ID, MAKER_ID)).willReturn(test);
        given(combinedTestReportExcelService.export(TEST_ID, MAKER_ID)).willReturn(new byte[]{1, 2, 3});
        given(fileStorageService.generateDownloadUrl("reports/excel/" + TEST_ID + ".xlsx", filename))
                .willReturn("https://blob.example.com/reports/excel/" + TEST_ID + ".xlsx");

        TestReportExcelDownload result = testReportExcelService.export(TEST_ID, MAKER_ID);

        assertThat(result.downloadUrl()).isEqualTo("https://blob.example.com/reports/excel/" + TEST_ID + ".xlsx");
        assertThat(result.filename()).isEqualTo(filename);
        verify(fileStorageService).upload(
                eq("reports/excel/" + TEST_ID + ".xlsx"),
                eq(new byte[]{1, 2, 3}),
                eq("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        );
        verify(reportExcelExportSupport).saveExcelKey(TEST_ID, "reports/excel/" + TEST_ID + ".xlsx");
    }

    @org.junit.jupiter.api.Test
    void excelKey가_있으면_재생성_없이_캐싱된_URL을_반환한다() {
        server.MATE.domain.test.entity.Test test = completedTest("reports/excel/" + TEST_ID + ".xlsx");
        String filename = "mate-report-" + TEST_ID + ".xlsx";
        given(reportExcelExportSupport.requireExportReadyTest(TEST_ID, MAKER_ID)).willReturn(test);
        given(fileStorageService.generateDownloadUrl("reports/excel/" + TEST_ID + ".xlsx", filename))
                .willReturn("https://blob.example.com/cached.xlsx");

        TestReportExcelDownload result = testReportExcelService.export(TEST_ID, MAKER_ID);

        assertThat(result.downloadUrl()).isEqualTo("https://blob.example.com/cached.xlsx");
        assertThat(result.filename()).isEqualTo(filename);
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
