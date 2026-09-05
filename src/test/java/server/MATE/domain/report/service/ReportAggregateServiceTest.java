package server.MATE.domain.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.entity.Report;
import server.MATE.domain.report.event.ReportAggregateService;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.report.service.ReportHandler;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.repository.TestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
    }

    @Test
    @DisplayName("완전한 리포트가 이미 존재하면 COMPLETED로 복구하고 기존 리포트를 반환한다")
    void recover_whenCompleteReportsAlreadyExist_returnsExistingReportsAndMarksCompleted() {
        Report report1 = report(101L);
        Report report2 = report(102L);

        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(2L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(2L);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of(report1, report2));

        List<Report> recovered = reportAggregateService.recover(
                new DataIntegrityViolationException("duplicate key"), TEST_ID);

        assertThat(recovered).containsExactly(report1, report2);
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
    }

    @Test
    @DisplayName("일반 예외여도 완전한 리포트가 이미 존재하면 COMPLETED로 복구하고 기존 리포트를 반환한다")
    void recover_whenGenericExceptionButReportsAreComplete_returnsExistingReportsAndMarksCompleted() {
        Report report1 = report(101L);
        Report report2 = report(102L);

        given(questionRepository.countQuestionsInTest(TEST_ID)).willReturn(2L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(2L);
        given(testRepository.findActiveById(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of(report1, report2));

        List<Report> recovered = reportAggregateService.recover(
                new RuntimeException("aggregate failed"), TEST_ID);

        assertThat(recovered).containsExactly(report1, report2);
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
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

    private Report report(Long questionId) {
        return Report.builder()
                .testId(TEST_ID)
                .questionId(questionId)
                .questionType(QuestionType.SUBJECTIVE)
                .result(Map.of("texts", List.of("응답")))
                .build();
    }
}
