package server.MATE.toss.gateway;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledTossMessengerGateway implements TossMessengerGateway {

    @Override
    public void sendBulk(String templateSetCode, List<Long> tossUserKeys, Map<String, Object> context) {
        throw new UnsupportedOperationException("Toss messenger API is disabled. Set toss.api.enabled=true.");
    }
}
