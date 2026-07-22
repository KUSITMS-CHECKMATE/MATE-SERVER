package server.MATE.toss.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import server.MATE.toss.client.promotion.TossPromotionApiClient;
import server.MATE.toss.dto.response.TossPromotionExecutionStatus;

@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class TossHttpPromotionGateway implements TossPromotionGateway {

    private final TossPromotionApiClient tossPromotionApiClient;

    @Override
    public String issueKey(Long tossUserKey) {
        return tossPromotionApiClient.issueKey(tossUserKey).key();
    }

    @Override
    public void execute(Long tossUserKey, String promotionCode, String rewardKey, Integer rewardAmount) {
        tossPromotionApiClient.execute(tossUserKey, promotionCode, rewardKey, rewardAmount);
    }

    @Override
    public PromotionGatewayExecutionStatus getExecutionStatus(Long tossUserKey, String promotionCode, String rewardKey) {
        TossPromotionExecutionStatus status = tossPromotionApiClient.getExecutionStatus(tossUserKey, promotionCode, rewardKey);
        return toGatewayStatus(status);
    }

    private PromotionGatewayExecutionStatus toGatewayStatus(TossPromotionExecutionStatus status) {
        if (status == null) {
            throw new IllegalStateException("Toss promotion execution status is null.");
        }
        return switch (status) {
            case SUCCESS -> PromotionGatewayExecutionStatus.SUCCEEDED;
            case PENDING -> PromotionGatewayExecutionStatus.PENDING;
            case FAILED -> PromotionGatewayExecutionStatus.FAILED;
        };
    }
}
