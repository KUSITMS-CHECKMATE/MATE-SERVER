package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.MATE.domain.test.event.TestCompletedEvent;

@Component
@RequiredArgsConstructor
public class ReportAggregationEventListener {

    private final ReportAggregationService reportAggregationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTestCompleted(TestCompletedEvent event) {
        // answer 저장 -> testStatus, reportStatus 변경 보장
        // event 발행하면 report 집계를 시작
        reportAggregationService.aggregate(event.testId());
    }
}
