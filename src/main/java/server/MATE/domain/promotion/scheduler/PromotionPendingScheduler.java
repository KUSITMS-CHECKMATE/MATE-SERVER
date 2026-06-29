package server.MATE.domain.promotion.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.MATE.domain.promotion.service.PromotionPendingResolverService;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
public class PromotionPendingScheduler {

    private final PromotionPendingResolverService promotionPendingResolverService;

    @Scheduled(fixedDelay = 300_000)
    @SchedulerLock(name = "promotionPendingScheduler", lockAtMostFor = "PT4M")
    public void resolvePromotionPending() {
        log.info("PROMOTION PENDING 재조회 스케줄러 실행");
        promotionPendingResolverService.resolveAll();
    }
}
