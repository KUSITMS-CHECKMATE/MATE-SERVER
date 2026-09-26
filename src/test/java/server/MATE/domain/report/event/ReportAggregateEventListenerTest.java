package server.MATE.domain.report.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.test.event.TestCompleteEvent;
import server.MATE.global.discord.report.ReportAlertService;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportAggregateEventListenerTest {

    @Mock private ReportAggregateService reportAggregateService;
    @Mock private ReportAlertService reportAlertService;
    @InjectMocks private ReportAggregateEventListener listener;

    @Test
    @DisplayName("집계(recover 포함)에서 예외가 새면 처리 오류 알림을 보내고 삼킨다")
    void onTestCompleted_crash_notifies() {
        IllegalStateException crash = new IllegalStateException("tx");
        given(reportAggregateService.aggregate(15L)).willThrow(crash);

        assertThatCode(() -> listener.onTestCompleted(new TestCompleteEvent(15L))).doesNotThrowAnyException();
        verify(reportAlertService).notifyAggregationCrashed(15L, crash);
    }

    @Test
    @DisplayName("정상 집계면 알림 없음")
    void onTestCompleted_success_noAlert() {
        listener.onTestCompleted(new TestCompleteEvent(15L));

        verify(reportAlertService, never()).notifyAggregationCrashed(any(), any());
    }
}
