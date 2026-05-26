package server.MATE.domain.report.service.excel.preparer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.report.excel.scale.ScaleReportExcelData;
import server.MATE.domain.report.service.excel.support.ReportExcelExportContext;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ScaleReportExcelServiceTest {

    @InjectMocks
    private ScaleReportExcelService scaleReportExcelService;

    @Test
    void prepareData는_척도_응답과_통계를_엑셀_데이터로_조립한다() {
        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.SCALE)
                .title("척도 질문")
                .sequence(1L)
                .build();
        ReflectionTestUtils.setField(question, "id", 20L);

        Scale scale = Scale.builder().range(7).build();
        ReflectionTestUtils.setField(scale, "id", 20L);

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

        ReportExcelExportContext context = ReportExcelExportContext.builder()
                .questionById(Map.of(20L, question))
                .reportResultByQuestionId(Map.of(20L, reportResult))
                .answersByQuestionId(Map.of(20L, answers))
                .scaleByQuestionId(Map.of(20L, scale))
                .build();

        ScaleReportExcelData data = scaleReportExcelService.prepareData(context, 20L);

        assertThat(data.respondents()).hasSize(2);
        assertThat(data.valueStats()).hasSize(7);
        assertThat(data.average()).isEqualTo("5");
    }

    @Test
    void 척도가_아니면_prepareData를_허용하지_않는다() {
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

        assertThatThrownBy(() -> scaleReportExcelService.prepareData(context, 20L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_005));
    }
}
