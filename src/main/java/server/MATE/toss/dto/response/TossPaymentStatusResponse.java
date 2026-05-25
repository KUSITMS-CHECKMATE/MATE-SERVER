package server.MATE.toss.dto.response;

import server.MATE.domain.payment.entity.PayMethod;
import server.MATE.domain.payment.entity.PayStatus;

import java.time.LocalDateTime;

public record TossPaymentStatusResponse(
        String payToken,
        String orderNo,
        PayStatus payStatus,
        PayMethod payMethod,
        int amount,
        int paidAmount,
        String transactionId,
        LocalDateTime approvalTime
) {
}
