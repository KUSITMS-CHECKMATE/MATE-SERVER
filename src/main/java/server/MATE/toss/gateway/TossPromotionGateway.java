package server.MATE.toss.gateway;

public interface TossPromotionGateway {

    String issueKey(Long tossUserKey);

    void execute(Long tossUserKey, String promotionCode, String rewardKey, Integer rewardAmount);

    PromotionGatewayExecutionStatus getExecutionStatus(Long tossUserKey, String promotionCode, String rewardKey);
}
