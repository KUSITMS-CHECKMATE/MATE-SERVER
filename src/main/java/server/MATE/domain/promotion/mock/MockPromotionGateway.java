package server.MATE.domain.promotion.mock;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;
import server.MATE.toss.dto.request.TossPromotionExecuteRequest;
import server.MATE.toss.dto.request.TossPromotionGetKeyRequest;
import server.MATE.toss.dto.request.TossPromotionResultRequest;
import server.MATE.toss.dto.response.TossPromotionExecuteResponse;
import server.MATE.toss.dto.response.TossPromotionExecutionStatus;
import server.MATE.toss.dto.response.TossPromotionKeyResponse;
import server.MATE.toss.dto.response.TossPromotionResultResponse;
import server.MATE.toss.gateway.TossPromotionGateway;

@Component
public class MockPromotionGateway implements TossPromotionGateway {

    @Override
    public TossPromotionKeyResponse getKey(TossPromotionGetKeyRequest request) {
        return new TossPromotionKeyResponse(encode("promotion:" + request.tossUserKey()));
    }

    @Override
    public TossPromotionExecuteResponse executePromotion(TossPromotionExecuteRequest request) {
        return new TossPromotionExecuteResponse(request.key());
    }

    @Override
    public TossPromotionResultResponse getExecutionResult(TossPromotionResultRequest request) {
        return new TossPromotionResultResponse(TossPromotionExecutionStatus.SUCCESS);
    }

    private String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
