package server.MATE.domain.payment.dto.response;

public record PaymentCreateResponse(
        Long paymentId,
        Long draftId,
        String orderNo,
        int amount,
        String payToken,
        boolean isTestPayment
) {
}
