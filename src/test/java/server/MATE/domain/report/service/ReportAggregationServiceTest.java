package server.MATE.domain.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.entity.Report;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.repository.TestRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReportAggregationServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private server.MATE.domain.answer.repository.AnswerRepository answerRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private TestRepository testRepository;

    private ReportAggregationService reportAggregationService;

    private server.MATE.domain.test.entity.Test test;
    private final Long TEST_ID = 10L;

    @BeforeEach
    void setUp() {
        reportAggregationService = new ReportAggregationService(
                questionRepository,
                answerRepository,
                reportRepository,
                testRepository,
                List.of()
        );

        test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of())
                .build();
        ReflectionTestUtils.setField(test, "id", TEST_ID);
    }

    @Test
    void recover_완전한_리포트가_이미_존재하면_COMPLETED로_복구하고_기존_리포트를_반환한다() {
        Report report1 = report(101L);
        Report report2 = report(102L);

        given(questionRepository.countByTestIdAndDeletedAtIsNull(TEST_ID)).willReturn(2L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(2L);
        given(testRepository.findByIdAndDeletedAtIsNull(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of(report1, report2));

        List<Report> recovered = reportAggregationService.recover(
                new DataIntegrityViolationException("duplicate key"), TEST_ID);

        assertThat(recovered).containsExactly(report1, report2);
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
    }

    @Test
    void recover_일반_예외여도_완전한_리포트가_이미_존재하면_COMPLETED로_복구하고_기존_리포트를_반환한다() {
        Report report1 = report(101L);
        Report report2 = report(102L);

        given(questionRepository.countByTestIdAndDeletedAtIsNull(TEST_ID)).willReturn(2L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(2L);
        given(testRepository.findByIdAndDeletedAtIsNull(TEST_ID)).willReturn(Optional.of(test));
        given(reportRepository.findAllByTestId(TEST_ID)).willReturn(List.of(report1, report2));

        List<Report> recovered = reportAggregationService.recover(
                new RuntimeException("aggregate failed"), TEST_ID);

        assertThat(recovered).containsExactly(report1, report2);
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
    }

    @Test
    void recover_부분_생성된_리포트만_존재하면_FAILED로_복구하고_빈_리스트를_반환한다() {
        given(questionRepository.countByTestIdAndDeletedAtIsNull(TEST_ID)).willReturn(3L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(1L);
        given(testRepository.findByIdAndDeletedAtIsNull(TEST_ID)).willReturn(Optional.of(test));

        List<Report> recovered = reportAggregationService.recover(
                new DataIntegrityViolationException("duplicate key"), TEST_ID);

        assertThat(recovered).isEmpty();
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.FAILED);
    }

    @Test
    void recover_일반_예외이고_리포트가_없으면_FAILED로_복구하고_빈_리스트를_반환한다() {
        given(questionRepository.countByTestIdAndDeletedAtIsNull(TEST_ID)).willReturn(2L);
        given(reportRepository.countByTestId(TEST_ID)).willReturn(0L);
        given(testRepository.findByIdAndDeletedAtIsNull(TEST_ID)).willReturn(Optional.of(test));

        List<Report> recovered = reportAggregationService.recover(
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
