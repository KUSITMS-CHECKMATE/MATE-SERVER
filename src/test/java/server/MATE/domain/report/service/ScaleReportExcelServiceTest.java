package server.MATE.domain.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.ScaleReportExcelData;
import server.MATE.domain.report.excel.ScaleReportExcelWriter;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScaleReportExcelServiceTest {

    @Mock
    private ReportExcelExportSupport reportExcelExportSupport;

    @Mock
    private ScaleRepository scaleRepository;

    @Mock
    private ScaleReportExcelWriter scaleReportExcelWriter;

    @InjectMocks
    private ScaleReportExcelService scaleReportExcelService;

    @Test
    void 메이커는_척도_통계_엑셀을_다운로드할_수_있다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .build();

        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.SCALE)
                .title("척도 질문")
                .sequence(1L)
                .build();

        Scale scale = Scale.builder().range(7).build();

        List<Answer> answers = List.of(
                Answer.builder().participationId(100L).questionId(20L).questionType(QuestionType.SCALE)
                        .answer(Map.of("value", 3)).build(),
                Answer.builder().participationId(101L).questionId(20L).questionType(QuestionType.SCALE)
                        .answer(Map.of("value", 7)).build()
        );

        Map<String, Object> reportResult = Map.of(
                "average", 5.0,
                "distribution", List.of(
                        Map.of("score", 1, "count", 0),
                        Map.of("score", 2, "count", 0),
                        Map.of("score", 3, "count", 1),
                        Map.of("score", 4, "count", 0),
                        Map.of("score", 5, "count", 0),
                        Map.of("score", 6, "count", 0),
                        Map.of("score", 7, "count", 1)
                )
        );

        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L)).willReturn(test);
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.SCALE, BaseErrorCode.REPORT_005))
                .willReturn(question);
        given(reportExcelExportSupport.requireReportResult(10L, 20L)).willReturn(reportResult);
        given(scaleRepository.findById(20L)).willReturn(java.util.Optional.of(scale));
        given(reportExcelExportSupport.loadAnswers(20L)).willReturn(answers);
        given(scaleReportExcelWriter.write(any(ScaleReportExcelData.class))).willReturn(new byte[]{1, 2, 3});

        TestReportExcelDownload download = scaleReportExcelService.export(10L, 20L, 1L);

        assertThat(download.filename()).isEqualTo("mate-scale-report-10-q01.xlsx");
        assertThat(download.content()).containsExactly(1, 2, 3);
        verify(scaleReportExcelWriter).write(any(ScaleReportExcelData.class));
    }

    @Test
    void 척도가_아니면_다운로드를_허용하지_않는다() {
        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .willReturn(server.MATE.domain.test.entity.Test.builder().makerId(1L).build());
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.SCALE, BaseErrorCode.REPORT_005))
                .willThrow(new BaseException(BaseErrorCode.REPORT_005));

        assertThatThrownBy(() -> scaleReportExcelService.export(10L, 20L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_005));
    }
}
