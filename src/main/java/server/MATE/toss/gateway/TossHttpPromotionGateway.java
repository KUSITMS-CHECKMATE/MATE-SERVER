package server.MATE.toss.gateway;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.dto.response.TossPromotionExecuteResponse;
import server.MATE.toss.dto.response.TossPromotionExecutionStatus;
import server.MATE.toss.dto.response.TossPromotionKeyResponse;

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
    public String issueKey(Long tossUserKey) {
        TossPromotionKeyResponse response = tossHttpClient.post(
                GET_KEY_PATH,
                Map.of(),
                headers -> headers.set(TOSS_USER_KEY_HEADER, String.valueOf(tossUserKey)),
                TossPromotionKeyResponse.class
        );
        return response.key();
    }

    @Override
    public void execute(Long tossUserKey, String promotionCode, String rewardKey, Integer rewardAmount) {
        var body = new ExecuteBody(promotionCode, rewardKey, rewardAmount);
        tossHttpClient.post(
                EXECUTE_PATH,
                body,
                headers -> headers.set(TOSS_USER_KEY_HEADER, String.valueOf(tossUserKey)),
                TossPromotionExecuteResponse.class
        );
    }

    @Override
    public PromotionGatewayExecutionStatus getExecutionStatus(Long tossUserKey, String promotionCode, String rewardKey) {
        var body = new ResultBody(promotionCode, rewardKey);
        TossPromotionExecutionStatus status = tossHttpClient.post(
                RESULT_PATH,
                body,
                headers -> headers.set(TOSS_USER_KEY_HEADER, String.valueOf(tossUserKey)),
                TossPromotionExecutionStatus.class
        );
        return toGatewayStatus(status);
    }

    private PromotionGatewayExecutionStatus toGatewayStatus(TossPromotionExecutionStatus status) {
        return switch (status) {
            case SUCCESS -> PromotionGatewayExecutionStatus.SUCCEEDED;
            case PENDING -> PromotionGatewayExecutionStatus.PENDING;
            case FAILED -> PromotionGatewayExecutionStatus.FAILED;
        };
    }

    private record ExecuteBody(String promotionCode, String key, Integer amount) {}

    private record ResultBody(String promotionCode, String key) {}
}
