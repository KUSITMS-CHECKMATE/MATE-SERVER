package server.MATE.toss.dto.request;

public record TossPaymentCreateRequest(
        String orderNo,
        int amount,
        boolean isTestPayment
) {
}
