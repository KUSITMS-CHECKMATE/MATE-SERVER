package server.MATE.domain.payment.dto.response;

import server.MATE.toss.dto.response.IapOrderStatus;

public record PaymentOrderStatusResponse(
        IapOrderStatus status,
        String reason,
        String statusDeterminedAt
) {
}
