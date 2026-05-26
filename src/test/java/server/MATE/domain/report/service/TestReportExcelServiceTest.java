package server.MATE.domain.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestReportExcelServiceTest {

    @Mock
    private CombinedTestReportExcelService combinedTestReportExcelService;

    @InjectMocks
    private TestReportExcelService testReportExcelService;

    @Test
    void 통합_엑셀_다운로드를_위임한다() {
        TestReportExcelDownload download = new TestReportExcelDownload(new byte[]{1, 2, 3}, "mate-report-10.xlsx");
        given(combinedTestReportExcelService.export(10L, 1L)).willReturn(download);

        TestReportExcelDownload result = testReportExcelService.export(10L, 1L);

        assertThat(result.filename()).isEqualTo("mate-report-10.xlsx");
        assertThat(result.content()).containsExactly(1, 2, 3);
        verify(combinedTestReportExcelService).export(10L, 1L);
    }
}
