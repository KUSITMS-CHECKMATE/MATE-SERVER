package server.MATE.domain.report.service.excel.preparer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.excel.subjective.SubjectiveReportExcelData;
import server.MATE.domain.report.service.excel.support.ReportExcelExportContext;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SubjectiveReportExcelServiceTest {

    @InjectMocks
    private SubjectiveReportExcelService subjectiveReportExcelService;

    @Test
    void prepareData는_주관식_응답을_엑셀_데이터로_조립한다() {
        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.SUBJECTIVE)
                .title("주관식 질문")
                .sequence(1L)
                .build();
        ReflectionTestUtils.setField(question, "id", 20L);

        Answer answer = Answer.builder()
                .participationId(100L)
                .questionId(20L)
                .questionType(QuestionType.SUBJECTIVE)
                .answer(Map.of("text", "불편해요"))
                .build();

        ReportExcelExportContext context = ReportExcelExportContext.builder()
                .questionById(Map.of(20L, question))
                .reportResultByQuestionId(Map.of(20L, Map.of("texts", List.of("불편해요"))))
                .answersByQuestionId(Map.of(20L, List.of(answer)))
                .build();

        SubjectiveReportExcelData data = subjectiveReportExcelService.prepareData(context, 20L);

        assertThat(data.respondents()).hasSize(1);
        assertThat(data.respondents().get(0).answerContent()).isEqualTo("불편해요");
    }

    @Test
    void 주관식이_아니면_prepareData를_허용하지_않는다() {
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

        assertThatThrownBy(() -> subjectiveReportExcelService.prepareData(context, 20L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_003));
    }
}
