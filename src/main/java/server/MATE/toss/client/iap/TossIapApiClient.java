package server.MATE.toss.client.iap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.dto.request.IapOrderStatusRequest;
import server.MATE.toss.dto.response.IapOrderStatusResponse;

@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class TossIapApiClient {

    private static final String TOSS_USER_KEY_HEADER = "x-toss-user-key";
    private static final String ORDER_STATUS_PATH = "/api-partner/v1/apps-in-toss/order/get-order-status";

    private final TossHttpClient tossHttpClient;

    public IapOrderStatusResponse getOrderStatus(Long tossUserKey, String orderId) {
        return tossHttpClient.post(
                ORDER_STATUS_PATH,
                new IapOrderStatusRequest(orderId),
                headers -> headers.set(TOSS_USER_KEY_HEADER, String.valueOf(tossUserKey)),
                IapOrderStatusResponse.class
        );
    }
}
