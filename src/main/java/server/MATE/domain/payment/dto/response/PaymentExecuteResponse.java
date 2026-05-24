package server.MATE.domain.payment.dto.response;

import server.MATE.domain.payment.entity.PayMethod;
import server.MATE.domain.payment.entity.PayStatus;

import java.time.LocalDateTime;

public record PaymentExecuteResponse(
        Long paymentId,
        Long draftId,
        PayStatus payStatus,
        String orderNo,
        int amount,
        int paidAmount,
        String payToken,
        String transactionId,
        PayMethod payMethod,
        LocalDateTime approvalTime
) {
}
