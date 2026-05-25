package server.MATE.domain.payment.dto.response;

import server.MATE.domain.payment.entity.PayStatus;

import java.time.LocalDateTime;

public record PaymentRefundResponse(
        Long paymentId,
        String refundNo,
        int refundedAmount,
        String transactionId,
        String payToken,
        PayStatus payStatus,
        LocalDateTime approvalTime
) {
}
