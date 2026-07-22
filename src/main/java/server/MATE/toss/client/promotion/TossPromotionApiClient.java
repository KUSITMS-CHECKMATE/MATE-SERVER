package server.MATE.toss.client.promotion;

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
public class TossPromotionApiClient {

    private static final String TOSS_USER_KEY_HEADER = "x-toss-user-key";
    private static final String GET_KEY_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion/get-key";
    private static final String EXECUTE_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion";
    private static final String RESULT_PATH = "/api-partner/v1/apps-in-toss/promotion/execution-result";

    private final TossHttpClient tossHttpClient;

    public TossPromotionKeyResponse issueKey(Long tossUserKey) {
        return tossHttpClient.post(
                GET_KEY_PATH,
                Map.of(),
                headers -> headers.set(TOSS_USER_KEY_HEADER, String.valueOf(tossUserKey)),
                TossPromotionKeyResponse.class
        );
    }

    public TossPromotionExecuteResponse execute(Long tossUserKey, String promotionCode, String rewardKey, Integer rewardAmount) {
        return tossHttpClient.post(
                EXECUTE_PATH,
                new ExecuteBody(promotionCode, rewardKey, rewardAmount),
                headers -> headers.set(TOSS_USER_KEY_HEADER, String.valueOf(tossUserKey)),
                TossPromotionExecuteResponse.class
        );
    }

    public TossPromotionExecutionStatus getExecutionStatus(Long tossUserKey, String promotionCode, String rewardKey) {
        return tossHttpClient.post(
                RESULT_PATH,
                new ResultBody(promotionCode, rewardKey),
                headers -> headers.set(TOSS_USER_KEY_HEADER, String.valueOf(tossUserKey)),
                TossPromotionExecutionStatus.class
        );
    }

    private record ExecuteBody(String promotionCode, String key, Integer amount) {}

    private record ResultBody(String promotionCode, String key) {}
}
