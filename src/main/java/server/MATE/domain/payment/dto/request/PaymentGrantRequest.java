package server.MATE.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentGrantRequest(
        @NotBlank String orderId,
        @NotNull Long draftId
) {
}
