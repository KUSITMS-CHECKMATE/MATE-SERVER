package server.MATE.domain.payment.dto.response;

import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;

import java.time.format.DateTimeFormatter;

public record PaymentHistoryResponse(
        String approvedAt,
        String testTitle,
        Integer amount,
        PayStatus payStatus,
        String orderNo
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static PaymentHistoryResponse of(Payment payment, String testTitle) {
        String formattedDate = payment.getApprovedAt() != null
                ? payment.getApprovedAt().format(FORMATTER)
                : null;
        return new PaymentHistoryResponse(
                formattedDate,
                testTitle,
                payment.getAmount(),
                payment.getPayStatus(),
                payment.getOrderNo()
        );
    }
}
