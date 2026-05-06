package server.MATE.domain.auth.dto.request;

import jakarta.validation.constraints.NotNull;

public record TestTokenRequest(
        @NotNull(message = "userId는 필수입니다.")
        Long userId
) {
}
