package server.MATE.domain.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.ObjectiveReportExcelData;
import server.MATE.domain.report.excel.ObjectiveReportExcelWriter;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ObjectiveReportExcelServiceTest {

    @Mock
    private ReportExcelExportSupport reportExcelExportSupport;

    @Mock
    private ObjectiveRepository objectiveRepository;

    @Mock
    private ObjectiveReportExcelWriter objectiveReportExcelWriter;

    @InjectMocks
    private ObjectiveReportExcelService objectiveReportExcelService;

    @Test
    void 메이커는_객관식_통계_엑셀을_다운로드할_수_있다() {
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
        given(objectiveReportExcelWriter.write(any(ObjectiveReportExcelData.class))).willReturn(new byte[]{1, 2, 3});

        TestReportExcelDownload download = objectiveReportExcelService.export(10L, 20L, 1L);

        assertThat(download.filename()).isEqualTo("mate-objective-report-10-q01.xlsx");
        assertThat(download.content()).containsExactly(1, 2, 3);
        verify(objectiveReportExcelWriter).write(any(ObjectiveReportExcelData.class));
    }

    @Test
    void 객관식이_아니면_다운로드를_허용하지_않는다() {
        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .willReturn(server.MATE.domain.test.entity.Test.builder().makerId(1L).build());
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.OBJECTIVE, BaseErrorCode.REPORT_002))
                .willThrow(new BaseException(BaseErrorCode.REPORT_002));

        assertThatThrownBy(() -> objectiveReportExcelService.export(10L, 20L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_002));
    }
}
