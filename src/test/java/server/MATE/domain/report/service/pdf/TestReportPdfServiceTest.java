package server.MATE.domain.report.service.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.report.config.MatePdfProperties;
import server.MATE.domain.report.dto.response.TestReportPdfDownload;
import server.MATE.domain.report.service.excel.support.ReportExcelExportSupport;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.storage.FileStorageService;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestReportPdfServiceTest {

    private static final Long TEST_ID = 10L;
    private static final Long MAKER_ID = 1L;
    private static final String AUTHORIZATION = "Bearer token";

    @Mock
    private ReportExcelExportSupport reportExcelExportSupport;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private WebClient matePdfWebClient;
    @Mock
    private MatePdfProperties matePdfProperties;

    private TestReportPdfService testReportPdfService;

    @BeforeEach
    void setUp() {
        testReportPdfService = new TestReportPdfService(
                reportExcelExportSupport,
                new ObjectMapper(),
                matePdfProperties,
                matePdfWebClient,
                fileStorageService
        );
    }

    @Test
    void pdfKey가_있으면_재생성_없이_캐싱된_bytes를_반환한다() {
        String pdfKey = "reports/pdf/" + TEST_ID + ".pdf";
        server.MATE.domain.test.entity.Test test = completedTest(pdfKey);
        byte[] cachedBytes = new byte[]{1, 2, 3};
        given(reportExcelExportSupport.requireExportReadyTest(TEST_ID, MAKER_ID)).willReturn(test);
        given(fileStorageService.download(pdfKey)).willReturn(cachedBytes);

        TestReportPdfDownload result = testReportPdfService.export(TEST_ID, MAKER_ID, AUTHORIZATION);

        assertThat(result.data()).isEqualTo(cachedBytes);
        assertThat(result.filename()).isEqualTo("mate-report-" + TEST_ID + ".pdf");
        verify(fileStorageService, never()).upload(any(), any(byte[].class), any());
        verify(matePdfWebClient, never()).get();
    }

    private server.MATE.domain.test.entity.Test completedTest(String pdfKey) {
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
        if (pdfKey != null) {
            test.savePdfKey(pdfKey);
        }
        return test;
    }
}
