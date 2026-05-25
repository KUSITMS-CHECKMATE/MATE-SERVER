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
import server.MATE.domain.report.dto.response.ReportResponse;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
                .build();
        ReflectionTestUtils.setField(test, "id", 10L);
    }

    @Test
    @DisplayName("완료 전 테스트는 현재 reportStatus를 유지해서 반환한다")
    void getReport_returnsCurrentReportStatusForNonCompletedTest() {
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.FAILED);

        given(testRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(test));
        given(questionRepository.findQuestionSummariesByTestId(10L)).willReturn(List.of());

        ReportResponse response = reportService.getReport(10L, 1L);

        assertThat(response.testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
        assertThat(response.reportStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(response.reports()).isEmpty();
    }

    @Test
    @DisplayName("반려된 테스트도 reportStatus를 덮어쓰지 않고 반환한다")
    void getReport_returnsCurrentReportStatusForRejectedTest() {
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.REJECTED);
        ReflectionTestUtils.setField(test, "reportStatus", ReportStatus.FAILED);

        given(testRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(test));
        given(questionRepository.findQuestionSummariesByTestId(10L)).willReturn(List.of());

        ReportResponse response = reportService.getReport(10L, 1L);

        assertThat(response.testStatus()).isEqualTo(TestStatus.REJECTED);
        assertThat(response.reportStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(response.reports()).isEmpty();
    }
}
