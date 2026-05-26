package server.MATE.domain.report.service.excel.preparer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.excel.abtest.AbTestReportExcelData;
import server.MATE.domain.report.service.excel.support.ReportExcelExportContext;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AbTestReportExcelServiceTest {

    @InjectMocks
    private AbTestReportExcelService abTestReportExcelService;

    @Test
    void prepareData는_AB_테스트_통계를_엑셀_데이터로_조립한다() {
        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.AB_TEST)
                .title("AB 질문")
                .sequence(1L)
                .build();
        ReflectionTestUtils.setField(question, "id", 20L);

        Map<String, Object> reportResult = Map.of(
                "A", Map.of("count", 3, "ratio", 0.6),
                "B", Map.of("count", 2, "ratio", 0.4)
        );

        ReportExcelExportContext context = ReportExcelExportContext.builder()
                .questionById(Map.of(20L, question))
                .reportResultByQuestionId(Map.of(20L, reportResult))
                .build();

        AbTestReportExcelData data = abTestReportExcelService.prepareData(context, 20L);

        assertThat(data.totalCount()).isEqualTo(5);
        assertThat(data.versionACount()).isEqualTo(3);
        assertThat(data.versionBCount()).isEqualTo(2);
    }

    @Test
    void AB_테스트가_아니면_prepareData를_허용하지_않는다() {
        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.OBJECTIVE)
                .title("객관식")
                .sequence(1L)
                .build();
        ReflectionTestUtils.setField(question, "id", 20L);

        ReportExcelExportContext context = ReportExcelExportContext.builder()
                .questionById(Map.of(20L, question))
                .build();

        assertThatThrownBy(() -> abTestReportExcelService.prepareData(context, 20L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_004));
    }
}
