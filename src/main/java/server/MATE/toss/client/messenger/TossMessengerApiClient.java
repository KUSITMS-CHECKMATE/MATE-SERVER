package server.MATE.toss.client.messenger;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.dto.response.TossBulkSendMessageResponse;

@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class TossMessengerApiClient {

    private static final String SEND_BULK_MESSAGE_PATH = "/api-partner/v1/apps-in-toss/messenger/send-bulk-message";

    private final TossHttpClient tossHttpClient;

    // 비즈니스 오류(200 OK+FAIL)는 재시도해도 똑같이 실패하므로, 네트워크 실패이거나 토스 서버 5xx 응답일 때만 재시도한다.
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2),
            exceptionExpression = "#root.cause != null or #root.statusCode.is5xxServerError()"
    )
    public TossBulkSendMessageResponse sendBulk(String templateSetCode, List<Long> tossUserKeys, Map<String, Object> context) {
        List<BulkSendMessageContext> contextList = tossUserKeys.stream()
                .map(tossUserKey -> new BulkSendMessageContext(tossUserKey, context))
                .toList();

        return tossHttpClient.post(
                SEND_BULK_MESSAGE_PATH,
                new BulkSendMessageRequest(contextList, templateSetCode),
                TossBulkSendMessageResponse.class
        );
    }

    private record BulkSendMessageRequest(List<BulkSendMessageContext> contextList, String templateSetCode) {}

    private record BulkSendMessageContext(Long userKey, Map<String, Object> context) {}
}
