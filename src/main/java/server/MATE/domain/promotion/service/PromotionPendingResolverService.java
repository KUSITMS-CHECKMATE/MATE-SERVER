package server.MATE.domain.promotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.entity.PromotionRewardStatus;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.toss.gateway.PromotionGatewayExecutionStatus;
import server.MATE.toss.gateway.TossPromotionGateway;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionPendingResolverService {

    private static final String ERROR_CODE_INVALID_STATE = "INVALID_PROMOTION_STATE";
    private static final String ERROR_REASON_INVALID_STATE = "Required fields are missing.";

    private final PromotionRewardRepository promotionRewardRepository;
    private final PromotionFailureStateService promotionFailureStateService;
    private final PromotionExecutionResultApplier promotionExecutionResultApplier;
    private final TossPromotionGateway tossPromotionGateway;

    public void resolveAll() {
        List<PromotionReward> pendingRewards = promotionRewardRepository.findTop100ByStatusIn(
                List.of(PromotionRewardStatus.EXECUTED, PromotionRewardStatus.PENDING)
        );

        for (PromotionReward reward : pendingRewards) {
            try {
                resolve(reward);
            } catch (Exception e) {
                // 상태를 FAILED로 바꾸지 않아 다음 스케줄러 실행에서 재시도된다.
                log.warn("PROMOTION PENDING resolve failed. rewardId={}", reward.getId(), e);
            }
        }
    }

    private void resolve(PromotionReward reward) {
        if (reward.getTossUserKey() == null || reward.getPromotionCode() == null || reward.getRewardKey() == null) {
            log.error("PROMOTION PENDING resolve skipped due to missing required fields. rewardId={}", reward.getId());
            promotionFailureStateService.markFailed(reward.getId(), ERROR_CODE_INVALID_STATE, ERROR_REASON_INVALID_STATE);
            return;
        }

        PromotionGatewayExecutionStatus status = tossPromotionGateway.getExecutionStatus(
                reward.getTossUserKey(), reward.getPromotionCode(), reward.getRewardKey()
        );
        if (status == PromotionGatewayExecutionStatus.PENDING && reward.getStatus() == PromotionRewardStatus.PENDING) {
            log.debug("PROMOTION still PENDING. rewardId={}", reward.getId());
            return;
        }
        promotionExecutionResultApplier.apply(reward.getId(), status);
    }
}
