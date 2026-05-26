package server.MATE.domain.promotion.event;

public record PromotionRewardRequestEvent(
        Long participationId,
        Long testId,
        Long testerId,
        Integer rewardAmount
) {
}
