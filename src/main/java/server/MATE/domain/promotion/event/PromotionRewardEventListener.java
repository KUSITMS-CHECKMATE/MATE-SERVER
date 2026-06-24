package server.MATE.domain.promotion.event;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.MATE.domain.promotion.service.PromotionService;

@Component
@RequiredArgsConstructor
public class PromotionRewardEventListener {

    private final PromotionService promotionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromotionRewardRequested(PromotionRewardRequestEvent event) {
        promotionService.grant(
                event.participationId(),
                event.testId(),
                event.testerId(),
                event.rewardAmount()
        );
    }
}
