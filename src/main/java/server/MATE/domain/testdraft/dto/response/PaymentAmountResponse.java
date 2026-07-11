package server.MATE.domain.testdraft.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.payment.policy.PaymentAmountBreakdown;

@Schema(description = "결제 금액 내역")
public record PaymentAmountResponse(
        @Schema(description = "테스터 리워드 총액", example = "10000")
        int testerRewardAmount,
        @Schema(description = "수수료", example = "6667")
        int feeAmount,
        @Schema(description = "부가세", example = "1667")
        int vatAmount,
        @Schema(description = "최종 결제 금액", example = "18334")
        int totalAmount
) {
    public static PaymentAmountResponse from(PaymentAmountBreakdown breakdown) {
        return new PaymentAmountResponse(
                breakdown.testerRewardAmount(),
                breakdown.feeAmount(),
                breakdown.vatAmount(),
                breakdown.totalAmount()
        );
    }
}
