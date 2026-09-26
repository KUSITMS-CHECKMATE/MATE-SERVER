package server.MATE.global.discord.report;

import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.MATE.domain.report.event.ReportAggregationFailedEvent;
import server.MATE.domain.report.event.ReportAiDegradedEvent;
import server.MATE.domain.report.event.ReportCompletedEvent;
import server.MATE.domain.report.event.ReportReaggregationRequestedEvent;

@Component
@RequiredArgsConstructor
public class ReportAlertEventListener {

    private final ReportAlertService reportAlertService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAggregationFailed(ReportAggregationFailedEvent event) {
        reportAlertService.notifyAggregationFailed(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAiDegraded(ReportAiDegradedEvent event) {
        reportAlertService.notifyAiDegraded(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportCompleted(ReportCompletedEvent event) {
        reportAlertService.notifyReportCompleted(event.testId());
    }

    // 집계 시작(비동기)보다 먼저 "재집계 중" 표시를 끝내 순서 역전 방지용으로 동기·최우선 실행
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReaggregationRequested(ReportReaggregationRequestedEvent event) {
        reportAlertService.notifyReaggregationRequested(event);
    }
}
