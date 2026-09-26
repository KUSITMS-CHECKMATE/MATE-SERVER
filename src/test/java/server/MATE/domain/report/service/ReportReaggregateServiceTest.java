package server.MATE.domain.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.report.event.ReportReaggregationRequestedEvent;
import server.MATE.domain.report.repository.ReportRepository;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.event.TestCompleteEvent;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportReaggregateServiceTest {

    private static final Long TEST_ID = 15L;

    @Mock private TestRepository testRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ReportReaggregateService service;
    private server.MATE.domain.test.entity.Test test;

    @BeforeEach
    void setUp() {
        service = new ReportReaggregateService(testRepository, reportRepository, eventPublisher);
        test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L).title("t").goalPpl(10).reward(300).testStatus(TestStatus.IN_PROGRESS)
                .closedAt(LocalDateTime.of(2026, 9, 27, 23, 59, 59)).build();
        ReflectionTestUtils.setField(test, "id", TEST_ID);
        test.complete();
        test.startReportAggregation();
        test.failReportAggregation();
    }

    @Test
    @DisplayName("FAILED면 리포트를 지우고 IN_PROGRESS로 바꾼 뒤 요청·집계 이벤트를 차례로 발행")
    void reaggregate_deletesReportsAndRestarts() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));

        ReportStatus status = service.reaggregate(TEST_ID, "🛠️ 관리자 API로");

        assertThat(status).isEqualTo(ReportStatus.IN_PROGRESS);
        assertThat(test.getReportStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
        verify(reportRepository).deleteAllByTestId(TEST_ID);
    }

    @Test
    @DisplayName("재집계 요청 이벤트가 집계 이벤트보다 먼저 발행된다")
    void reaggregate_publishesRequestedBeforeCompleteEvent() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));

        service.reaggregate(TEST_ID, "🛠️ 관리자 API로");

        InOrder inOrder = inOrder(eventPublisher);
        inOrder.verify(eventPublisher).publishEvent(new ReportReaggregationRequestedEvent(TEST_ID, "🛠️ 관리자 API로"));
        inOrder.verify(eventPublisher).publishEvent(new TestCompleteEvent(TEST_ID));
    }

    @Test
    @DisplayName("리포트가 FAILED가 아니면 REPORT_013")
    void reaggregate_notFailed_throws() {
        test.restartReportAggregation();
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));

        assertThatThrownBy(() -> service.reaggregate(TEST_ID, "x"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(BaseErrorCode.REPORT_013);
        verifyNoInteractions(reportRepository, eventPublisher);
    }

    @Test
    @DisplayName("없으면 TEST_004, 테스트가 COMPLETED가 아니면 TEST_007")
    void reaggregate_rejects() {
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.reaggregate(TEST_ID, "x"))
                .extracting(e -> ((BaseException) e).getErrorCode()).isEqualTo(BaseErrorCode.TEST_004);

        server.MATE.domain.test.entity.Test inProgress = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L).title("t").testStatus(TestStatus.IN_PROGRESS)
                .closedAt(LocalDateTime.of(2026, 9, 27, 23, 59, 59)).build();
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(inProgress));
        assertThatThrownBy(() -> service.reaggregate(TEST_ID, "x"))
                .extracting(e -> ((BaseException) e).getErrorCode()).isEqualTo(BaseErrorCode.TEST_007);
    }
}
