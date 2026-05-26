package server.MATE.domain.promotion.event;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.MATE.domain.promotion.service.MockPromotionService;

@Component
@RequiredArgsConstructor
public class PromotionRewardEventListener {

    private final MockPromotionService mockPromotionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromotionRewardRequested(PromotionRewardRequestEvent event) {
        mockPromotionService.grant(
                event.participationId(),
                event.testId(),
                event.testerId(),
                event.rewardAmount()
        );
    }
}
