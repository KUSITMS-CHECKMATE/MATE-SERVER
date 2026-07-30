package server.MATE.domain.payment.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.MATE.domain.payment.service.PaymentPendingResolverService;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
public class PaymentPendingScheduler {

    private final PaymentPendingResolverService paymentPendingResolverService;

    @Scheduled(fixedDelay = 300_000)
    @SchedulerLock(name = "paymentPendingScheduler", lockAtMostFor = "PT4M")
    public void resolvePaymentPending() {
        log.info("PAYMENT PENDING 재시도 스케줄러 실행");
        paymentPendingResolverService.resolveAll();
    }
}
