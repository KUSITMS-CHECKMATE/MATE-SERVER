package server.MATE.domain.report.event;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.toss.gateway.TossMessengerGateway;

@Component
@RequiredArgsConstructor
public class ReportCompletedEventListener {

    // TODO: 앱인토스 콘솔에 기능성 캠페인 등록 후 발급받은 실제 발송 코드로 교체 (#315)
    private static final String REPORT_COMPLETED_TEMPLATE_CODE = "mate-report-completed";

    private final TossAccountRepository tossAccountRepository;
    private final TossMessengerGateway tossMessengerGateway;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReportCompleted(ReportCompletedEvent event) {
        TossAccount tossAccount = tossAccountRepository.findByUserId(event.makerId()).orElse(null);
        if (tossAccount == null || !tossAccount.isLinked()) {
            return;
        }

        tossMessengerGateway.sendSingle(
                REPORT_COMPLETED_TEMPLATE_CODE,
                tossAccount.getTossUserKey(),
                Map.of(
                        "testId", String.valueOf(event.testId()),
                        "title", event.title()
                )
        );
    }
}
