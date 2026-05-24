package server.MATE.toss.dto.response;

import java.time.LocalDateTime;

public record TossPaymentRefundResponse(
        String refundNo,
        LocalDateTime approvalTime,
        int refundedAmount,
        String payToken,
        String transactionId
) {
}
