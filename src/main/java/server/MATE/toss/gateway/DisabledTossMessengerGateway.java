package server.MATE.toss.gateway;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledTossMessengerGateway implements TossMessengerGateway {

    @Override
    public void sendBulk(String templateSetCode, List<Long> tossUserKeys, Map<String, Object> context) {
        log.warn("토스 메신저 API가 비활성화되어 벌크 발송을 건너뜁니다. templateSetCode={}, targetCount={}",
                templateSetCode, tossUserKeys.size());
    }

    @Override
    public void sendSingle(String templateSetCode, Long tossUserKey, Map<String, Object> context) {
        log.warn("토스 메신저 API가 비활성화되어 단건 발송을 건너뜁니다. templateSetCode={}, tossUserKey={}",
                templateSetCode, tossUserKey);
    }
}
