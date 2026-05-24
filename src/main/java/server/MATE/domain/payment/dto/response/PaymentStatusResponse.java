package server.MATE.domain.payment.dto.response;

import server.MATE.domain.payment.entity.PayMethod;
import server.MATE.domain.payment.entity.PayStatus;

import java.time.LocalDateTime;

public record PaymentStatusResponse(
        Long paymentId,
        Long draftId,
        Long testId,
        String orderNo,
        String payToken,
        PayStatus payStatus,
        PayMethod payMethod,
        Integer amount,
        Integer paidAmount,
        String transactionId,
        LocalDateTime approvalTime
) {
}
