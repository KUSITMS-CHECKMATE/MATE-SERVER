package server.MATE.toss.gateway;

import java.time.LocalDateTime;

public record IapOrderStatusResult(
        IapOrderState status,
        String sku,
        String reason,
        LocalDateTime statusDeterminedAt
) {
}
