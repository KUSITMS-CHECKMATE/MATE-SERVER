package server.MATE.toss.dto.response;

import server.MATE.domain.payment.entity.PayMethod;

import java.time.LocalDateTime;

public record TossPaymentExecuteResponse(
        String orderNo,
        int amount,
        LocalDateTime approvalTime,
        int paidAmount,
        PayMethod payMethod,
        String payToken,
        String transactionId,
        String accountBankCode,
        String cardCompanyCode
) {
}
