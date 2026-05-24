package server.MATE.toss.dto.request;

public record TossPaymentStatusRequest(
        String payToken,
        String orderNo,
        boolean isTestPayment
) {
}
