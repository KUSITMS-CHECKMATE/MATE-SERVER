package server.MATE.domain.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.AbTestReportExcelData;
import server.MATE.domain.report.excel.AbTestReportExcelWriter;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AbTestReportExcelServiceTest {

    @Mock
    private ReportExcelExportSupport reportExcelExportSupport;

    @Mock
    private AbTestReportExcelWriter abTestReportExcelWriter;

    @InjectMocks
    private AbTestReportExcelService abTestReportExcelService;

    @Test
    void 메이커는_AB테스트_통계_엑셀을_다운로드할_수_있다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .build();

        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.AB_TEST)
                .title("AB 질문")
                .sequence(1L)
                .build();

        Map<String, Object> reportResult = Map.of(
                "A", Map.of("count", 2, "ratio", 0.67),
                "B", Map.of("count", 1, "ratio", 0.33)
        );

        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L)).willReturn(test);
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.AB_TEST, BaseErrorCode.REPORT_004))
                .willReturn(question);
        given(reportExcelExportSupport.requireReportResult(10L, 20L)).willReturn(reportResult);
        given(abTestReportExcelWriter.write(any(AbTestReportExcelData.class))).willReturn(new byte[]{1, 2, 3});

        TestReportExcelDownload download = abTestReportExcelService.export(10L, 20L, 1L);

        assertThat(download.filename()).isEqualTo("mate-abtest-report-10-q01.xlsx");
        assertThat(download.content()).containsExactly(1, 2, 3);
        verify(abTestReportExcelWriter).write(any(AbTestReportExcelData.class));
    }

    @Test
    void AB테스트가_아니면_다운로드를_허용하지_않는다() {
        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .willReturn(server.MATE.domain.test.entity.Test.builder().makerId(1L).build());
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.AB_TEST, BaseErrorCode.REPORT_004))
                .willThrow(new BaseException(BaseErrorCode.REPORT_004));

        assertThatThrownBy(() -> abTestReportExcelService.export(10L, 20L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_004));
    }
}
