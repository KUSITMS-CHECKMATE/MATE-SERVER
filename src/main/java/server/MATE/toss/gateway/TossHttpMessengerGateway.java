package server.MATE.toss.gateway;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import server.MATE.toss.client.messenger.TossMessengerApiClient;
import server.MATE.toss.dto.response.TossMessengerSendResponse;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class TossHttpMessengerGateway implements TossMessengerGateway {

    private static final int MAX_BATCH_SIZE = 2500;

    private final TossMessengerApiClient tossMessengerApiClient;

    @Override
    public void sendBulk(String templateSetCode, List<Long> tossUserKeys, Map<String, Object> context) {
        for (int fromIndex = 0; fromIndex < tossUserKeys.size(); fromIndex += MAX_BATCH_SIZE) {
            int toIndex = Math.min(fromIndex + MAX_BATCH_SIZE, tossUserKeys.size());
            List<Long> batch = tossUserKeys.subList(fromIndex, toIndex);
            try {
                TossMessengerSendResponse response = tossMessengerApiClient.sendBulk(templateSetCode, batch, context);
                logIfPartiallyDelivered(templateSetCode, batch.size(), response);
            } catch (RuntimeException e) {
                log.warn("토스 메신저 배치 발송에 실패했습니다. templateSetCode={}, batchStart={}, batchSize={}",
                        templateSetCode, fromIndex, batch.size(), e);
            }
        }
    }

    private void logIfPartiallyDelivered(String templateSetCode, int requestedCount, TossMessengerSendResponse response) {
        if (response.msgCount() < requestedCount) {
            log.warn("토스 메신저 발송이 일부 대상에게 전달되지 않았습니다. templateSetCode={}, requested={}, delivered={}",
                    templateSetCode, requestedCount, response.msgCount());
        }
    }

    @Override
    public void sendSingle(String templateSetCode, Long tossUserKey, Map<String, Object> context) {
        try {
            TossMessengerSendResponse response = tossMessengerApiClient.send(templateSetCode, tossUserKey, context);
            logIfPartiallyDelivered(templateSetCode, 1, response);
        } catch (RuntimeException e) {
            log.warn("토스 메신저 단건 발송에 실패했습니다. templateSetCode={}, tossUserKey={}",
                    templateSetCode, tossUserKey, e);
        }
    }
}
