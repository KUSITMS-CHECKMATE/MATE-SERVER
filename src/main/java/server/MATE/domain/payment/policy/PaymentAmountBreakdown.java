package server.MATE.domain.payment.policy;

public record PaymentAmountBreakdown(
        int testerRewardAmount,
        int feeAmount,
        int vatAmount,
        int totalAmount
) {
}
