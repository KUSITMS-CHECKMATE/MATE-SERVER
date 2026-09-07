package server.MATE.domain.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.report.dto.response.ReportResponse;
import server.MATE.domain.report.entity.Report;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.domain.users.entity.Role;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportService reportService;

    private server.MATE.domain.test.entity.Test test;

    @BeforeEach
    void setUp() {
        test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of())
                .testStatus(TestStatus.WAITING)
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();
        ReflectionTestUtils.setField(test, "id", 10L);
    }

    @Test
    @DisplayName("완료 전 테스트는 현재 reportStatus를 유지해서 반환한다")
    void getReport_returnsCurrentReportStatusForNonCompletedTest() {
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.FAILED);

        given(testRepository.findActiveById(10L)).willReturn(Optional.of(test));
        given(questionRepository.countQuestionsInTest(10L)).willReturn(0L);

        ReportResponse response = reportService.getReport(10L, 1L, Role.USER);

        assertThat(response.testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
        assertThat(response.reportStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(response.reports()).isEmpty();
        verify(questionRepository, never()).findQuestionSummariesInTest(10L);
    }

    @Test
    @DisplayName("응답에 목표 인원 대비 달성률이 포함된다")
    void getReport_includesAchievementRate() {
        ReflectionTestUtils.setField(test, "goalPpl", 50);
        ReflectionTestUtils.setField(test, "pplCount", 12L);
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.WAITING);

        given(testRepository.findActiveById(10L)).willReturn(Optional.of(test));
        given(questionRepository.countQuestionsInTest(10L)).willReturn(0L);

        ReportResponse response = reportService.getReport(10L, 1L, Role.USER);

        assertThat(response.achievementRate()).isEqualTo(0.24);
    }

    @Test
    @DisplayName("반려된 테스트도 reportStatus를 덮어쓰지 않고 반환한다")
    void getReport_returnsCurrentReportStatusForRejectedTest() {
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.REJECTED);
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.FAILED);

        given(testRepository.findActiveById(10L)).willReturn(Optional.of(test));
        given(questionRepository.countQuestionsInTest(10L)).willReturn(0L);

        ReportResponse response = reportService.getReport(10L, 1L, Role.USER);

        assertThat(response.testStatus()).isEqualTo(TestStatus.REJECTED);
        assertThat(response.reportStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(response.reports()).isEmpty();
        verify(questionRepository, never()).findQuestionSummariesInTest(10L);
    }

    @Test
    @DisplayName("리포트 집계가 완료되지 않은 COMPLETED 테스트는 질문 요약을 조회하지 않고 빈 결과를 반환한다")
    void getReport_returnsEarlyWithoutQuestionSummariesWhenReportStatusIsNotCompleted() {
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.COMPLETED);
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.IN_PROGRESS);

        given(testRepository.findActiveById(10L)).willReturn(Optional.of(test));
        given(questionRepository.countQuestionsInTest(10L)).willReturn(2L);

        ReportResponse response = reportService.getReport(10L, 1L, Role.USER);

        assertThat(response.testStatus()).isEqualTo(TestStatus.COMPLETED);
        assertThat(response.reportStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
        assertThat(response.questionCount()).isEqualTo(2);
        assertThat(response.reports()).isEmpty();
        verify(questionRepository, never()).findQuestionSummariesInTest(10L);
    }

    @Test
    @DisplayName("리포트 집계가 완료된 테스트는 질문 요약을 조회해 reports를 조립한다")
    void getReport_fetchesQuestionSummariesWhenReportStatusIsCompleted() {
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.COMPLETED);
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.COMPLETED);

        Report report = Report.builder()
                .testId(10L)
                .questionId(101L)
                .result(Map.of("count", 3))
                .build();

        given(testRepository.findActiveById(10L)).willReturn(Optional.of(test));
        given(questionRepository.countQuestionsInTest(10L)).willReturn(1L);
        given(reportRepository.findAllByTestId(10L)).willReturn(List.of(report));
        given(questionRepository.findQuestionSummariesInTest(10L)).willReturn(List.of(
                new QuestionSummaryItem(101L, 1L, "질문", server.MATE.domain.question.entity.QuestionType.SUBJECTIVE)
        ));

        ReportResponse response = reportService.getReport(10L, 1L, Role.USER);

        assertThat(response.reportStatus()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(response.reports()).hasSize(1);
        verify(questionRepository).findQuestionSummariesInTest(10L);
    }

    @Test
    @DisplayName("활성 질문의 리포트가 하나라도 누락되면 FAILED를 반환한다")
    void getReport_returnsFailedWhenActiveQuestionReportIsMissing() {
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.COMPLETED);
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.COMPLETED);

        Report report = Report.builder()
                .testId(10L)
                .questionId(101L)
                .result(Map.of("count", 3))
                .build();

        given(testRepository.findActiveById(10L)).willReturn(Optional.of(test));
        given(questionRepository.countQuestionsInTest(10L)).willReturn(2L);
        given(reportRepository.findAllByTestId(10L)).willReturn(List.of(report));
        given(questionRepository.findQuestionSummariesInTest(10L)).willReturn(List.of(
                new QuestionSummaryItem(101L, 1L, "질문1", server.MATE.domain.question.entity.QuestionType.SUBJECTIVE),
                new QuestionSummaryItem(102L, 2L, "질문2", server.MATE.domain.question.entity.QuestionType.SUBJECTIVE)
        ));

        ReportResponse response = reportService.getReport(10L, 1L, Role.USER);

        assertThat(response.reportStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(response.reports()).isEmpty();
    }

    @Test
    @DisplayName("고립된 리포트가 추가로 있어도 활성 질문 리포트가 모두 있으면 정상 반환한다")
    void getReport_ignoresOrphanReportsWhenActiveQuestionReportsExist() {
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.COMPLETED);
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.COMPLETED);

        Report activeReport = Report.builder()
                .testId(10L)
                .questionId(101L)
                .result(Map.of("count", 3))
                .build();
        Report orphanReport = Report.builder()
                .testId(10L)
                .questionId(999L)
                .result(Map.of("count", 1))
                .build();

        given(testRepository.findActiveById(10L)).willReturn(Optional.of(test));
        given(questionRepository.countQuestionsInTest(10L)).willReturn(1L);
        given(reportRepository.findAllByTestId(10L)).willReturn(List.of(activeReport, orphanReport));
        given(questionRepository.findQuestionSummariesInTest(10L)).willReturn(List.of(
                new QuestionSummaryItem(101L, 1L, "질문", server.MATE.domain.question.entity.QuestionType.SUBJECTIVE)
        ));

        ReportResponse response = reportService.getReport(10L, 1L, Role.USER);

        assertThat(response.reportStatus()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(response.reports()).hasSize(1);
        assertThat(response.reports().getFirst().questionId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("메이커가 아닌 일반 사용자는 리포트를 조회할 수 없다")
    void getReport_throwsExceptionWhenNotMaker() {
        given(testRepository.findActiveById(10L)).willReturn(Optional.of(test));

        BaseException exception = org.junit.jupiter.api.Assertions.assertThrows(BaseException.class,
                () -> reportService.getReport(10L, 999L, Role.USER));

        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.TEST_005);
    }

    @Test
    @DisplayName("어드민은 메이커가 아니어도 리포트를 조회할 수 있다")
    void getReport_adminCanViewAnyReport() {
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.PENDING);

        given(testRepository.findActiveById(10L)).willReturn(Optional.of(test));
        given(questionRepository.countQuestionsInTest(10L)).willReturn(0L);

        ReportResponse response = reportService.getReport(10L, 999L, Role.ADMIN);

        assertThat(response.testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
    }
}
