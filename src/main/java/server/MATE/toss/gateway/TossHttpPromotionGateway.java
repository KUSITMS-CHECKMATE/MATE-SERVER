package server.MATE.toss.gateway;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.dto.request.TossPromotionExecuteRequest;
import server.MATE.toss.dto.request.TossPromotionGetKeyRequest;
import server.MATE.toss.dto.request.TossPromotionResultRequest;
import server.MATE.toss.dto.response.TossPromotionExecuteResponse;
import server.MATE.toss.dto.response.TossPromotionExecutionStatus;
import server.MATE.toss.dto.response.TossPromotionKeyResponse;
import server.MATE.toss.dto.response.TossPromotionResultResponse;

@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class TossHttpPromotionGateway implements TossPromotionGateway {

    private static final String TOSS_USER_KEY_HEADER = "x-toss-user-key";
    private static final String GET_KEY_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion/get-key";
    private static final String EXECUTE_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion";
    private static final String RESULT_PATH = "/api-partner/v1/apps-in-toss/promotion/execution-result";

    private final TossHttpClient tossHttpClient;

    @Override
    public TossPromotionKeyResponse getKey(TossPromotionGetKeyRequest request) {
        return tossHttpClient.post(
                GET_KEY_PATH,
                Map.of(),
                headers -> headers.set(TOSS_USER_KEY_HEADER, String.valueOf(request.tossUserKey())),
                TossPromotionKeyResponse.class
        );
    }

    @Override
    public TossPromotionExecuteResponse executePromotion(TossPromotionExecuteRequest request) {
        var body = new ExecuteBody(request.promotionCode(), request.key(), request.amount());
        return tossHttpClient.post(
                EXECUTE_PATH,
                body,
                headers -> headers.set(TOSS_USER_KEY_HEADER, String.valueOf(request.tossUserKey())),
                TossPromotionExecuteResponse.class
        );
    }

    @Override
    public TossPromotionResultResponse getExecutionResult(TossPromotionResultRequest request) {
        var body = new ResultBody(request.promotionCode(), request.key());
        TossPromotionExecutionStatus status = tossHttpClient.post(
                RESULT_PATH,
                body,
                headers -> headers.set(TOSS_USER_KEY_HEADER, String.valueOf(request.tossUserKey())),
                TossPromotionExecutionStatus.class
        );
        return new TossPromotionResultResponse(status);
    }

    private record ExecuteBody(String promotionCode, String key, Integer amount) {}

    private record ResultBody(String promotionCode, String key) {}
}
