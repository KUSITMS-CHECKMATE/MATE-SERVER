package server.MATE.domain.promotion.dto.response;

import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.entity.PromotionRewardStatus;

public record PromotionRetryResponse(
        Long rewardId,
        PromotionRewardStatus status,
        String lastErrorCode,
        String lastErrorReason
) {
    public static PromotionRetryResponse from(PromotionReward reward) {
        return new PromotionRetryResponse(
                reward.getId(),
                reward.getStatus(),
                reward.getLastErrorCode(),
                reward.getLastErrorReason()
        );
    }
}
