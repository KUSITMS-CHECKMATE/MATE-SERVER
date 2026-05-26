package server.MATE.toss.dto.request;

public record TossPaymentExecuteRequest(
        String payToken,
        String orderNo,
        boolean isTestPayment
) {
}
