package server.MATE.toss.gateway;

import java.util.List;
import java.util.Map;

public interface TossMessengerGateway {

    void sendBulk(String templateSetCode, List<Long> tossUserKeys, Map<String, Object> context);
}
