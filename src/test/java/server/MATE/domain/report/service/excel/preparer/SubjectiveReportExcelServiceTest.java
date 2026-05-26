package server.MATE.domain.report.service.excel.preparer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.excel.subjective.SubjectiveReportExcelData;
import server.MATE.domain.report.service.excel.support.ReportExcelExportSupport;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SubjectiveReportExcelServiceTest {

    @Mock
    private ReportExcelExportSupport reportExcelExportSupport;

    @InjectMocks
    private SubjectiveReportExcelService subjectiveReportExcelService;

    @Test
    void prepareData는_주관식_응답을_엑셀_데이터로_조립한다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .build();

        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.SUBJECTIVE)
                .title("주관식 질문")
                .sequence(2L)
                .build();

        Answer answer = Answer.builder()
                .participationId(100L)
                .questionId(20L)
                .questionType(QuestionType.SUBJECTIVE)
                .answer(Map.of("text", "좋아요"))
                .build();

        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L)).willReturn(test);
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.SUBJECTIVE, BaseErrorCode.REPORT_003))
                .willReturn(question);
        given(reportExcelExportSupport.requireReportResult(10L, 20L)).willReturn(Map.of("answers", List.of()));
        given(reportExcelExportSupport.loadAnswers(20L)).willReturn(List.of(answer));

        SubjectiveReportExcelData data = subjectiveReportExcelService.prepareData(10L, 20L, 1L);

        assertThat(data.questionNumberLabel()).isEqualTo("Q02");
        assertThat(data.respondents()).hasSize(1);
        assertThat(data.respondents().get(0).answerContent()).isEqualTo("좋아요");
    }

    @Test
    void 주관식이_아니면_prepareData를_허용하지_않는다() {
        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .willReturn(server.MATE.domain.test.entity.Test.builder().makerId(1L).build());
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.SUBJECTIVE, BaseErrorCode.REPORT_003))
                .willThrow(new BaseException(BaseErrorCode.REPORT_003));

        assertThatThrownBy(() -> subjectiveReportExcelService.prepareData(10L, 20L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_003));
    }
}
