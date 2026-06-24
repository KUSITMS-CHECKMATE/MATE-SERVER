package server.MATE.toss.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import server.MATE.toss.dto.request.TossPromotionExecuteRequest;
import server.MATE.toss.dto.request.TossPromotionGetKeyRequest;
import server.MATE.toss.dto.request.TossPromotionResultRequest;
import server.MATE.toss.dto.response.TossPromotionExecuteResponse;
import server.MATE.toss.dto.response.TossPromotionKeyResponse;
import server.MATE.toss.dto.response.TossPromotionResultResponse;

@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledTossPromotionGateway implements TossPromotionGateway {

    @Override
    public TossPromotionKeyResponse getKey(TossPromotionGetKeyRequest request) {
        throw new UnsupportedOperationException("Toss promotion API is disabled. Set toss.api.enabled=true.");
    }

    @Override
    public TossPromotionExecuteResponse executePromotion(TossPromotionExecuteRequest request) {
        throw new UnsupportedOperationException("Toss promotion API is disabled. Set toss.api.enabled=true.");
    }

    @Override
    public TossPromotionResultResponse getExecutionResult(TossPromotionResultRequest request) {
        throw new UnsupportedOperationException("Toss promotion API is disabled. Set toss.api.enabled=true.");
    }
}
