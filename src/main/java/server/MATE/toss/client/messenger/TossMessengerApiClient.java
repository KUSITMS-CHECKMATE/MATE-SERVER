package server.MATE.toss.client.messenger;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.dto.response.TossMessengerSendResponse;

@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class TossMessengerApiClient {

    private static final String SEND_BULK_MESSAGE_PATH = "/api-partner/v1/apps-in-toss/messenger/send-bulk-message";
    private static final String SEND_MESSAGE_PATH = "/api-partner/v1/apps-in-toss/messenger/send-message";
    private static final String TOSS_USER_KEY_HEADER = "x-toss-user-key";

    private final TossHttpClient tossHttpClient;

    @TossMessengerApiRetryable
    public TossMessengerSendResponse sendBulk(String templateSetCode, List<Long> tossUserKeys, Map<String, Object> context) {
        List<BulkSendMessageContext> contextList = tossUserKeys.stream()
                .map(tossUserKey -> new BulkSendMessageContext(tossUserKey, context))
                .toList();

        return tossHttpClient.post(
                SEND_BULK_MESSAGE_PATH,
                new BulkSendMessageRequest(contextList, templateSetCode),
                TossMessengerSendResponse.class
        );
    }

    private record BulkSendMessageRequest(List<BulkSendMessageContext> contextList, String templateSetCode) {}

    private record BulkSendMessageContext(Long userKey, Map<String, Object> context) {}

    @TossMessengerApiRetryable
    public TossMessengerSendResponse send(String templateSetCode, Long tossUserKey, Map<String, Object> context) {
        return tossHttpClient.post(
                SEND_MESSAGE_PATH,
                new SendMessageRequest(templateSetCode, context),
                Map.of(TOSS_USER_KEY_HEADER, String.valueOf(tossUserKey)),
                TossMessengerSendResponse.class
        );
    }

    private record SendMessageRequest(String templateSetCode, Map<String, Object> context) {}
}
