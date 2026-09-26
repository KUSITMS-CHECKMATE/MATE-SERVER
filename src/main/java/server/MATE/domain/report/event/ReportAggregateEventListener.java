package server.MATE.domain.report.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.MATE.domain.test.event.TestCompleteEvent;
import server.MATE.global.discord.report.ReportAlertService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportAggregateEventListener {

    private final ReportAggregateService reportAggregateService;
    private final ReportAlertService reportAlertService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTestCompleted(TestCompleteEvent event) {
        // answer 저장 -> testStatus, reportStatus 변경 보장
        // event 발행하면 report 집계를 시작
        try {
            reportAggregateService.aggregate(event.testId());
        } catch (RuntimeException e) {
            // recover까지 실패한 경우라 리포트 상태가 IN_PROGRESS에 멈췄을 수 있음
            log.error("테스트 {} 리포트 집계 처리 오류", event.testId(), e);
            reportAlertService.notifyAggregationCrashed(event.testId(), e);
        }
    }
}

