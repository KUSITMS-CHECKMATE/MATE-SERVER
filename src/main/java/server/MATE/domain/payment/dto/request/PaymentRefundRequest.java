package server.MATE.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentRefundRequest(
        @NotBlank(message = "환불 사유는 필수입니다.")
        @Size(max = 100, message = "환불 사유는 최대 100자까지 입력 가능합니다.")
        String reason
) {
}
