package server.MATE.domain.report.event;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.MATE.domain.test.event.TestCompleteEvent;

@Component
@RequiredArgsConstructor
public class ReportAggregateEventListener {

    private final ReportAggregateService reportAggregateService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTestCompleted(TestCompleteEvent event) {
        // answer 저장 -> testStatus, reportStatus 변경 보장
        // event 발행하면 report 집계를 시작
        reportAggregateService.aggregate(event.testId());
    }
}
