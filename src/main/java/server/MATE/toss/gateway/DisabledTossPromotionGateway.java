package server.MATE.toss.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledTossPromotionGateway implements TossPromotionGateway {

    @Override
    public String issueKey(Long tossUserKey) {
        throw new UnsupportedOperationException("Toss promotion API is disabled. Set toss.api.enabled=true.");
    }

    @Override
    public void execute(Long tossUserKey, String promotionCode, String rewardKey, Integer rewardAmount) {
        throw new UnsupportedOperationException("Toss promotion API is disabled. Set toss.api.enabled=true.");
    }

    @Override
    public PromotionGatewayExecutionStatus getExecutionStatus(Long tossUserKey, String promotionCode, String rewardKey) {
        throw new UnsupportedOperationException("Toss promotion API is disabled. Set toss.api.enabled=true.");
    }
}
