package server.MATE.toss.dto.request;

public record TossPaymentRefundRequest(
        String payToken,
        String reason,
        boolean isTestPayment
) {
}
