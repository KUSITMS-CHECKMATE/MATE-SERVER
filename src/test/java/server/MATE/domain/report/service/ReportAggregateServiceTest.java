package server.MATE.domain.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.entity.Report;
import server.MATE.domain.report.event.ReportAggregateService;
import server.MATE.domain.report.event.ReportCompletedEvent;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.report.service.ReportHandler;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.repository.TestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReportAggregateServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private ReportHandler reportHandler;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReportAggregateService reportAggregateService;

    private server.MATE.domain.test.entity.Test test;
    private final Long TEST_ID = 10L;

    @BeforeEach
    void setUp() {
        given(reportHandler.supports()).willReturn(QuestionType.SUBJECTIVE);

        reportAggregateService = new ReportAggregateService(
                questionRepository,
                answerRepository,
                reportRepository,
                testRepository,
                eventPublisher,
                List.of(reportHandler)
        );

        test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of())
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();
        ReflectionTestUtils.setField(test, "id", TEST_ID);
    }

    @Test
    @DisplayName("aggregate는 활성 질문 목록을 조회해 handler 결과로 리포트를 저장하고 집계를 완료 처리한다")
    void aggregate_buildsReportsFromQuestionsAndAnswers() {
        Question question = Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.SUBJECTIVE)
                .title("주관식 질문")
                .description("설명")
                .sequence(1L)
                .build();
        ReflectionTestUtils.setField(question, "id", 101L);

        Answer answer = Answer.builder()
                .participationId(501L)
                .questionId(101L)
                .questionType(QuestionType.SUBJECTIVE)
                .answer(Map.of("text", "응답"))
                .build();

        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(1L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(0L);
        given(questionRepository.findQuestionsInTest(TEST_ID)).willReturn(List.of(question));
        given(answerRepository.findAllByQuestionIdInAndDeletedAtIsNull(List.of(101L))).willReturn(List.of(answer));
        given(reportHandler.compute(List.of(question), Map.of(101L, List.of(answer)), true))
                .willReturn(Map.of(101L, Map.of("texts", List.of("응답"))));
        given(reportRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        List<Report> result = reportAggregateService.aggregate(TEST_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getQuestionId()).isEqualTo(101L);
        assertThat(result.getFirst().getResult()).isEqualTo(Map.of("texts", List.of("응답")));
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
        verify(questionRepository).findQuestionsInTest(TEST_ID);
        verify(reportHandler).compute(List.of(question), Map.of(101L, List.of(answer)), true);
        verify(eventPublisher).publishEvent(new ReportCompletedEvent(TEST_ID, test.getMakerId(), test.getTitle()));
    }

    @Test
    @DisplayName("질문이 0개인 테스트가 처음 완료되면 리포트 완료 이벤트를 발행한다")
    void aggregate_whenZeroQuestions_publishesCompletedEvent() {
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(0L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(0L);
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of());

        List<Report> result = reportAggregateService.aggregate(TEST_ID);

        assertThat(result).isEmpty();
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
        verify(eventPublisher).publishEvent(new ReportCompletedEvent(TEST_ID, test.getMakerId(), test.getTitle()));
    }

    @Test
    @DisplayName("완료 확정 직전에 테스트가 삭제되면 완료 처리와 알림을 건너뛴다")
    void aggregate_whenTestDeletedRightBeforeCompletion_skipsWithoutThrowing() {
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.empty());
        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(0L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(0L);
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of());

        List<Report> result = reportAggregateService.aggregate(TEST_ID);

        assertThat(result).isEmpty();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("이미 완료 처리된 테스트를 다시 집계해도 완료 이벤트를 재발행하지 않는다")
    void aggregate_whenAlreadyCompleted_doesNotRepublishEvent() {
        test.completeReportAggregation();
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(2L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(2L);
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of(report(101L), report(102L)));

        List<Report> result = reportAggregateService.aggregate(TEST_ID);

        assertThat(result).hasSize(2);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("완전한 리포트가 이미 존재하면 COMPLETED로 복구하고 기존 리포트를 반환한다")
    void recover_whenCompleteReportsAlreadyExist_returnsExistingReportsAndMarksCompleted() {
        Report report1 = report(101L);
        Report report2 = report(102L);

        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(2L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(2L);
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of(report1, report2));

        List<Report> recovered = reportAggregateService.recover(
                new DataIntegrityViolationException("duplicate key"), TEST_ID);

        assertThat(recovered).containsExactly(report1, report2);
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
        verify(eventPublisher).publishEvent(new ReportCompletedEvent(TEST_ID, test.getMakerId(), test.getTitle()));
    }

    @Test
    @DisplayName("일반 예외여도 완전한 리포트가 이미 존재하면 COMPLETED로 복구하고 기존 리포트를 반환한다")
    void recover_whenGenericExceptionButReportsAreComplete_returnsExistingReportsAndMarksCompleted() {
        Report report1 = report(101L);
        Report report2 = report(102L);

        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(2L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(2L);
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of(report1, report2));

        List<Report> recovered = reportAggregateService.recover(
                new RuntimeException("aggregate failed"), TEST_ID);

        assertThat(recovered).containsExactly(report1, report2);
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
        verify(eventPublisher).publishEvent(new ReportCompletedEvent(TEST_ID, test.getMakerId(), test.getTitle()));
    }

    @Test
    @DisplayName("recover 시점에 이미 완료 상태였다면 완료 이벤트를 재발행하지 않는다")
    void recover_whenAlreadyCompleted_doesNotRepublishEvent() {
        test.completeReportAggregation();
        Report report1 = report(101L);
        Report report2 = report(102L);

        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(2L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(2L);
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of(report1, report2));

        List<Report> recovered = reportAggregateService.recover(
                new DataIntegrityViolationException("duplicate key"), TEST_ID);

        assertThat(recovered).containsExactly(report1, report2);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("부분 생성된 리포트만 존재하면 FAILED로 복구하고 빈 리스트를 반환한다")
    void recover_whenReportsArePartiallyCreated_returnsEmptyListAndMarksFailed() {
        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(3L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(1L);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));

        List<Report> recovered = reportAggregateService.recover(
                new DataIntegrityViolationException("duplicate key"), TEST_ID);

        assertThat(recovered).isEmpty();
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.FAILED);
    }

    @Test
    @DisplayName("일반 예외이고 리포트가 없으면 FAILED로 복구하고 빈 리스트를 반환한다")
    void recover_whenGenericExceptionAndNoReportsExist_returnsEmptyListAndMarksFailed() {
        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(2L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(0L);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));

        List<Report> recovered = reportAggregateService.recover(
                new RuntimeException("aggregate failed"), TEST_ID);

        assertThat(recovered).isEmpty();
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.FAILED);
    }

    @Test
    @DisplayName("재개 전에 만든 리포트가 있으면 새로 계산한 뒤 옛 리포트를 지우고 교체한다")
    void aggregate_whenReportsBeforeReopen_regeneratesAfterComputing() {
        LocalDateTime reopenedAt = LocalDateTime.of(2026, 9, 27, 1, 0);
        ReflectionTestUtils.setField(test, "reopenedAt", reopenedAt);
        test.startReportAggregation();
        Question question = subjectiveQuestion(101L);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.existsByTestIdAndCreatedAtBefore(TEST_ID, reopenedAt)).willReturn(true);
        given(questionRepository.findQuestionsInTest(TEST_ID)).willReturn(List.of(question));
        given(answerRepository.findAllByQuestionIdInAndDeletedAtIsNull(List.of(101L))).willReturn(List.of());
        given(reportHandler.compute(List.of(question), Map.of(), true))
                .willReturn(Map.of(101L, Map.of("texts", List.of("새 응답"))));
        given(reportRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        List<Report> result = reportAggregateService.aggregate(TEST_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getResult()).isEqualTo(Map.of("texts", List.of("새 응답")));
        InOrder inOrder = inOrder(reportHandler, reportRepository);
        inOrder.verify(reportHandler).compute(List.of(question), Map.of(), true);
        inOrder.verify(reportRepository).deleteAllByTestId(TEST_ID);
        inOrder.verify(reportRepository).saveAll(any());
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
        verify(eventPublisher).publishEvent(new ReportCompletedEvent(TEST_ID, test.getMakerId(), test.getTitle()));
        verify(reportRepository, never()).countByTestId(TEST_ID);
    }

    @Test
    @DisplayName("재개 뒤에 만든 리포트만 있으면 재집계하지 않고 기존 완료 분기를 따른다")
    void aggregate_whenReportsCreatedAfterReopen_skipsRegeneration() {
        LocalDateTime reopenedAt = LocalDateTime.of(2026, 9, 27, 1, 0);
        ReflectionTestUtils.setField(test, "reopenedAt", reopenedAt);
        test.completeReportAggregation();
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.existsByTestIdAndCreatedAtBefore(TEST_ID, reopenedAt)).willReturn(false);
        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(2L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(2L);
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of(report(101L), report(102L)));

        List<Report> result = reportAggregateService.aggregate(TEST_ID);

        assertThat(result).hasSize(2);
        verify(reportRepository, never()).deleteAllByTestId(any());
        verify(reportHandler, never()).compute(any(), any(), anyBoolean());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("재집계 계산이 실패하면 옛 리포트를 지우지 않는다")
    void aggregate_whenRegenerationComputeFails_keepsOldReports() {
        LocalDateTime reopenedAt = LocalDateTime.of(2026, 9, 27, 1, 0);
        ReflectionTestUtils.setField(test, "reopenedAt", reopenedAt);
        Question question = subjectiveQuestion(101L);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.existsByTestIdAndCreatedAtBefore(TEST_ID, reopenedAt)).willReturn(true);
        given(questionRepository.findQuestionsInTest(TEST_ID)).willReturn(List.of(question));
        given(answerRepository.findAllByQuestionIdInAndDeletedAtIsNull(List.of(101L))).willReturn(List.of());
        given(reportHandler.compute(List.of(question), Map.of(), true))
                .willThrow(new IllegalStateException("AI 호출 실패"));

        assertThatThrownBy(() -> reportAggregateService.aggregate(TEST_ID))
                .isInstanceOf(IllegalStateException.class);

        verify(reportRepository, never()).deleteAllByTestId(any());
        verify(reportRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("재개한 적 없는 테스트는 옛 리포트 여부를 조회하지 않는다")
    void aggregate_whenNeverReopened_doesNotCheckReportsBeforeReopen() {
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(0L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(0L);
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of());

        reportAggregateService.aggregate(TEST_ID);

        verify(reportRepository, never()).existsByTestIdAndCreatedAtBefore(any(), any());
    }

    @Test
    @DisplayName("재개 전 리포트만 남은 채 재집계가 3회 실패하면 FAILED로 복구하고 완료 알림을 보내지 않는다")
    void recover_whenOnlyReportsBeforeReopen_marksFailedWithoutNotification() {
        LocalDateTime reopenedAt = LocalDateTime.of(2026, 9, 27, 1, 0);
        ReflectionTestUtils.setField(test, "reopenedAt", reopenedAt);
        test.startReportAggregation();
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.existsByTestIdAndCreatedAtBefore(TEST_ID, reopenedAt)).willReturn(true);

        List<Report> recovered = reportAggregateService.recover(
                new IllegalStateException("AI 호출 실패"), TEST_ID);

        assertThat(recovered).isEmpty();
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.FAILED);
        verifyNoInteractions(eventPublisher);
        verify(testRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("재개 뒤에 만든 리포트만 있으면 recover도 완료로 확정한다")
    void recover_whenReportsCreatedAfterReopen_marksCompleted() {
        LocalDateTime reopenedAt = LocalDateTime.of(2026, 9, 27, 1, 0);
        ReflectionTestUtils.setField(test, "reopenedAt", reopenedAt);
        test.startReportAggregation();
        Report report1 = report(101L);
        Report report2 = report(102L);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.existsByTestIdAndCreatedAtBefore(TEST_ID, reopenedAt)).willReturn(false);
        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(2L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(2L);
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of(report1, report2));

        List<Report> recovered = reportAggregateService.recover(
                new DataIntegrityViolationException("duplicate key"), TEST_ID);

        assertThat(recovered).containsExactly(report1, report2);
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
        verify(eventPublisher).publishEvent(new ReportCompletedEvent(TEST_ID, test.getMakerId(), test.getTitle()));
    }

    private Question subjectiveQuestion(Long id) {
        Question question = Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.SUBJECTIVE)
                .title("주관식 질문")
                .description("설명")
                .sequence(1L)
                .build();
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private Report report(Long questionId) {
        return Report.builder()
                .testId(TEST_ID)
                .questionId(questionId)
                .questionType(QuestionType.SUBJECTIVE)
                .result(Map.of("texts", List.of("응답")))
                .build();
    }
}
