package server.MATE.domain.promotion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.entity.PromotionRewardStatus;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.toss.dto.request.TossPromotionResultRequest;
import server.MATE.toss.gateway.TossPromotionGateway;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionPendingResolverService {

    private static final String ERROR_CODE_EXECUTION_FAILED = "PROMOTION_EXECUTION_FAILED";
    private static final String ERROR_REASON_EXECUTION_FAILED = "Promotion execution result is FAILED.";

    private final PromotionRewardRepository promotionRewardRepository;
    private final PromotionExecuteStateService promotionExecuteStateService;
    private final PromotionFailureStateService promotionFailureStateService;
    private final TossPromotionGateway tossPromotionGateway;

    public void resolveAll() {
        List<PromotionReward> pendingRewards = promotionRewardRepository.findAllByStatusIn(
                List.of(PromotionRewardStatus.EXECUTED, PromotionRewardStatus.PENDING)
        );

        for (PromotionReward reward : pendingRewards) {
            try {
                resolve(reward);
            } catch (Exception e) {
                log.warn("PROMOTION PENDING resolve failed. rewardId={}", reward.getId(), e);
            }
        }
    }

    private void resolve(PromotionReward reward) {
        var result = tossPromotionGateway.getExecutionResult(
                new TossPromotionResultRequest(reward.getTossUserKey(), reward.getPromotionCode(), reward.getRewardKey())
        );

        switch (result.status()) {
            case SUCCESS -> {
                promotionExecuteStateService.markSucceeded(reward.getId());
                log.info("PROMOTION PENDING resolved to SUCCEEDED. rewardId={}", reward.getId());
            }
            case FAILED -> {
                promotionFailureStateService.markFailed(reward.getId(), ERROR_CODE_EXECUTION_FAILED, ERROR_REASON_EXECUTION_FAILED);
                log.warn("PROMOTION PENDING resolved to FAILED. rewardId={}", reward.getId());
            }
            case PENDING -> log.debug("PROMOTION still PENDING. rewardId={}", reward.getId());
        }
    }
}
