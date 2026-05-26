package server.MATE.domain.report.service.excel.preparer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.report.excel.objective.ObjectiveReportExcelData;
import server.MATE.domain.report.service.excel.support.ReportExcelExportSupport;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ObjectiveReportExcelServiceTest {

    @Mock
    private ReportExcelExportSupport reportExcelExportSupport;

    @Mock
    private ObjectiveRepository objectiveRepository;

    @InjectMocks
    private ObjectiveReportExcelService objectiveReportExcelService;

    @Test
    void prepareData는_객관식_응답과_통계를_엑셀_데이터로_조립한다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .build();

        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.OBJECTIVE)
                .title("객관식 질문")
                .sequence(1L)
                .build();

        ObjectiveOption option = ObjectiveOption.builder()
                .content("옵션1")
                .sequence(1)
                .build();
        ReflectionTestUtils.setField(option, "id", 1L);
        Objective objective = Objective.builder().question(question).build();
        objective.addOption(option);

        Answer answer = Answer.builder()
                .participationId(100L)
                .questionId(20L)
                .questionType(QuestionType.OBJECTIVE)
                .answer(Map.of("optionIds", List.of(1L)))
                .build();

        Map<String, Object> reportResult = Map.of(
                "options", List.of(Map.of("optionId", 1L, "content", "옵션1", "count", 1, "ratio", 1.0))
        );

        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L)).willReturn(test);
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.OBJECTIVE, BaseErrorCode.REPORT_002))
                .willReturn(question);
        given(reportExcelExportSupport.requireReportResult(10L, 20L)).willReturn(reportResult);
        given(objectiveRepository.findWithOptionsById(20L)).willReturn(java.util.Optional.of(objective));
        given(reportExcelExportSupport.loadAnswers(20L)).willReturn(List.of(answer));

        ObjectiveReportExcelData data = objectiveReportExcelService.prepareData(10L, 20L, 1L);

        assertThat(data.questionNumberLabel()).isEqualTo("Q01");
        assertThat(data.questionTitle()).isEqualTo("객관식 질문");
        assertThat(data.respondents()).hasSize(1);
        assertThat(data.optionStats()).hasSize(1);
        assertThat(data.totalResponses()).isEqualTo(1);
    }

    @Test
    void 객관식이_아니면_prepareData를_허용하지_않는다() {
        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .willReturn(server.MATE.domain.test.entity.Test.builder().makerId(1L).build());
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.OBJECTIVE, BaseErrorCode.REPORT_002))
                .willThrow(new BaseException(BaseErrorCode.REPORT_002));

        assertThatThrownBy(() -> objectiveReportExcelService.prepareData(10L, 20L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_002));
    }
}
