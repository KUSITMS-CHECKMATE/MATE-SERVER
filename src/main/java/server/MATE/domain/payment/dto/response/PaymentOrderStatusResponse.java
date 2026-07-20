package server.MATE.domain.payment.dto.response;

import java.time.LocalDateTime;

import server.MATE.toss.gateway.IapOrderState;

public record PaymentOrderStatusResponse(
        IapOrderState status,
        String reason,
        LocalDateTime statusDeterminedAt
) {
}
