package server.MATE.domain.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.entity.Report;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReportExcelExportSupportTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportExcelExportSupport reportExcelExportSupport;

    @Test
    void 테스트가_종료되지_않으면_엑셀_다운로드를_허용하지_않는다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .testStatus(TestStatus.IN_PROGRESS)
                .build();
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.PENDING);

        given(testRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(test));

        assertThatThrownBy(() -> reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(BaseErrorCode.TEST_006));
    }

    @Test
    void 리포트_집계가_완료되지_않으면_엑셀_다운로드를_허용하지_않는다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .testStatus(TestStatus.COMPLETED)
                .build();
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.IN_PROGRESS);

        given(testRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(test));

        assertThatThrownBy(() -> reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(BaseErrorCode.REPORT_007));
    }

    @Test
    void 집계_결과가_없으면_엑셀_통계를_제공하지_않는다() {
        given(reportRepository.findByTestIdAndQuestionId(10L, 20L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reportExcelExportSupport.requireReportResult(10L, 20L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(BaseErrorCode.REPORT_007));
    }

    @Test
    void 집계_완료_테스트는_리포트_결과를_반환한다() {
        Map<String, Object> result = Map.of("A", Map.of("count", 3, "ratio", 0.6));
        Report report = Report.builder()
                .testId(10L)
                .questionId(20L)
                .questionType(QuestionType.AB_TEST)
                .result(result)
                .build();

        given(reportRepository.findByTestIdAndQuestionId(10L, 20L)).willReturn(Optional.of(report));

        org.assertj.core.api.Assertions.assertThat(reportExcelExportSupport.requireReportResult(10L, 20L))
                .isEqualTo(result);
    }
}
