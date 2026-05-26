package server.MATE.domain.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.FiveSecondOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.FiveSecondObjectiveReportExcelData;
import server.MATE.domain.report.excel.FiveSecondObjectiveReportExcelWriter;
import server.MATE.domain.report.excel.FiveSecondSubjectiveReportExcelData;
import server.MATE.domain.report.excel.FiveSecondSubjectiveReportExcelWriter;
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
class FiveSecondReportExcelServiceTest {

    @Mock
    private ReportExcelExportSupport reportExcelExportSupport;

    @Mock
    private FiveSecondRepository fiveSecondRepository;

    @Mock
    private FiveSecondObjectiveReportExcelWriter fiveSecondObjectiveReportExcelWriter;

    @Mock
    private FiveSecondSubjectiveReportExcelWriter fiveSecondSubjectiveReportExcelWriter;

    @InjectMocks
    private FiveSecondReportExcelService fiveSecondReportExcelService;

    @Test
    void 객관식_5초_테스트_엑셀을_다운로드할_수_있다() {
        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.FIVE_SECOND)
                .title("화면에서 가장 눈에 띄는 요소는?")
                .sequence(1L)
                .build();

        FiveSecondOption option = FiveSecondOption.builder()
                .content("검색창")
                .sequence(1)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(option, "id", 1L);

        FiveSecond fiveSecond = FiveSecond.builder()
                .question(question)
                .imageKey("image-key")
                .imageRatio(server.MATE.domain.question.entity.ImageRatio.RATIO_4_3)
                .isObjective(true)
                .isOther(false)
                .build();
        fiveSecond.addOption(option);

        Answer answer = Answer.builder()
                .participationId(100L)
                .questionId(20L)
                .questionType(QuestionType.FIVE_SECOND)
                .answer(Map.of("optionIds", List.of(1L)))
                .build();

        Map<String, Object> reportResult = Map.of(
                "options", List.of(Map.of("optionId", 1L, "content", "검색창", "count", 1, "ratio", 1.0))
        );

        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .willReturn(server.MATE.domain.test.entity.Test.builder().makerId(1L).build());
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.FIVE_SECOND, BaseErrorCode.REPORT_009))
                .willReturn(question);
        given(fiveSecondRepository.findWithOptionsById(20L)).willReturn(java.util.Optional.of(fiveSecond));
        given(reportExcelExportSupport.requireReportResult(10L, 20L)).willReturn(reportResult);
        given(reportExcelExportSupport.loadAnswers(20L)).willReturn(List.of(answer));
        given(fiveSecondObjectiveReportExcelWriter.write(any(FiveSecondObjectiveReportExcelData.class)))
                .willReturn(new byte[]{1, 2, 3});

        TestReportExcelDownload download = fiveSecondReportExcelService.export(10L, 20L, 1L);

        assertThat(download.filename()).isEqualTo("mate-five-second-report-10-q01.xlsx");
        verify(fiveSecondObjectiveReportExcelWriter).write(any(FiveSecondObjectiveReportExcelData.class));
    }

    @Test
    void 주관식_5초_테스트_엑셀을_다운로드할_수_있다() {
        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.FIVE_SECOND)
                .title("5초간 본 화면을 설명해주세요.")
                .sequence(1L)
                .build();

        FiveSecond fiveSecond = FiveSecond.builder()
                .question(question)
                .imageKey("image-key")
                .imageRatio(server.MATE.domain.question.entity.ImageRatio.RATIO_4_3)
                .isObjective(false)
                .build();

        Answer answer = Answer.builder()
                .participationId(100L)
                .questionId(20L)
                .questionType(QuestionType.FIVE_SECOND)
                .answer(Map.of("text", "로고가 컸어요"))
                .build();

        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .willReturn(server.MATE.domain.test.entity.Test.builder().makerId(1L).build());
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.FIVE_SECOND, BaseErrorCode.REPORT_009))
                .willReturn(question);
        given(fiveSecondRepository.findWithOptionsById(20L)).willReturn(java.util.Optional.of(fiveSecond));
        given(reportExcelExportSupport.requireReportResult(10L, 20L)).willReturn(Map.of("texts", List.of()));
        given(reportExcelExportSupport.loadAnswers(20L)).willReturn(List.of(answer));
        given(fiveSecondSubjectiveReportExcelWriter.write(any(FiveSecondSubjectiveReportExcelData.class)))
                .willReturn(new byte[]{4, 5, 6});

        TestReportExcelDownload download = fiveSecondReportExcelService.export(10L, 20L, 1L);

        assertThat(download.content()).containsExactly(4, 5, 6);
        verify(fiveSecondSubjectiveReportExcelWriter).write(any(FiveSecondSubjectiveReportExcelData.class));
    }

    @Test
    void FIVE_SECOND가_아니면_다운로드를_허용하지_않는다() {
        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .willReturn(server.MATE.domain.test.entity.Test.builder().makerId(1L).build());
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.FIVE_SECOND, BaseErrorCode.REPORT_009))
                .willThrow(new BaseException(BaseErrorCode.REPORT_009));

        assertThatThrownBy(() -> fiveSecondReportExcelService.export(10L, 20L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_009));
    }
}
