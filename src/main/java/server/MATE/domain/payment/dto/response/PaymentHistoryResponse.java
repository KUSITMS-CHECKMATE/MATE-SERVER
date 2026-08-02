package server.MATE.domain.payment.dto.response;

import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;

import java.time.format.DateTimeFormatter;

public record PaymentHistoryResponse(
        String approvedAt,
        Long testId,
        String testTitle,
        TestStatus testStatus,
        String thumbnailUrl,
        Integer amount,
        PayStatus payStatus,
        String orderNo
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static PaymentHistoryResponse of(Payment payment, Test test, String thumbnailUrl) {
        String formattedDate = payment.getApprovedAt() != null
                ? payment.getApprovedAt().format(FORMATTER)
                : null;
        return new PaymentHistoryResponse(
                formattedDate,
                test != null ? test.getId() : null,
                test != null ? test.getTitle() : null,
                test != null ? test.getTestStatus() : null,
                thumbnailUrl,
                payment.getAmount(),
                payment.getPayStatus(),
                payment.getOrderNo()
        );
    }
}
