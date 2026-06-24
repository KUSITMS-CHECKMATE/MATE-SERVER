package server.MATE.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentRestoreRequest(
        @NotBlank String orderId,
        Long draftId
) {
}
