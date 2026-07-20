package server.MATE.toss.gateway;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import server.MATE.toss.client.iap.TossIapApiClient;
import server.MATE.toss.dto.response.IapOrderStatus;
import server.MATE.toss.dto.response.IapOrderStatusResponse;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class TossHttpIapGateway implements TossIapGateway {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TossIapApiClient tossIapApiClient;

    @Override
    public IapOrderStatusResult getOrderStatus(Long tossUserKey, String orderId) {
        IapOrderStatusResponse response = tossIapApiClient.getOrderStatus(tossUserKey, orderId);
        return new IapOrderStatusResult(
                toGatewayState(response.status()),
                response.sku(),
                response.reason(),
                parseStatusDeterminedAt(response.statusDeterminedAt())
        );
    }

    private IapOrderState toGatewayState(IapOrderStatus status) {
        if (status == null) {
            throw new IllegalStateException("Toss IAP order status is null.");
        }
        return switch (status) {
            case PURCHASED -> IapOrderState.PURCHASED;
            case PAYMENT_COMPLETED -> IapOrderState.PAYMENT_COMPLETED;
            case FAILED -> IapOrderState.FAILED;
            case REFUNDED -> IapOrderState.REFUNDED;
            case ORDER_IN_PROGRESS -> IapOrderState.ORDER_IN_PROGRESS;
            case NOT_FOUND -> IapOrderState.NOT_FOUND;
            case MINIAPP_MISMATCH -> IapOrderState.MINIAPP_MISMATCH;
            case ERROR -> IapOrderState.ERROR;
        };
    }

    private LocalDateTime parseStatusDeterminedAt(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now(KST);
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            try {
                return OffsetDateTime.parse(value).atZoneSameInstant(KST).toLocalDateTime();
            } catch (DateTimeParseException ex) {
                log.warn("Unrecognized statusDeterminedAt format='{}', falling back to now", value);
                return LocalDateTime.now(KST);
            }
        }
    }
}
