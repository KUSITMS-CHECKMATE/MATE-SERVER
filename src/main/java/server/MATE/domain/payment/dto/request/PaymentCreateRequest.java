package server.MATE.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record PaymentCreateRequest(
        @NotNull(message = "draftId는 필수입니다.")
        Long draftId,

        Boolean isTestPayment
) {
}
