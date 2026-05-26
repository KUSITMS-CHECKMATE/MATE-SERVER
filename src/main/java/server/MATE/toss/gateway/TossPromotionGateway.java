package server.MATE.toss.gateway;

import server.MATE.toss.dto.request.TossPromotionExecuteRequest;
import server.MATE.toss.dto.request.TossPromotionGetKeyRequest;
import server.MATE.toss.dto.request.TossPromotionResultRequest;
import server.MATE.toss.dto.response.TossPromotionExecuteResponse;
import server.MATE.toss.dto.response.TossPromotionKeyResponse;
import server.MATE.toss.dto.response.TossPromotionResultResponse;

public interface TossPromotionGateway {

    TossPromotionKeyResponse getKey(TossPromotionGetKeyRequest request);

    TossPromotionExecuteResponse executePromotion(TossPromotionExecuteRequest request);

    TossPromotionResultResponse getExecutionResult(TossPromotionResultRequest request);
}
