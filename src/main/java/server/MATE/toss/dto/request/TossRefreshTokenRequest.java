package server.MATE.toss.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TossRefreshTokenRequest(
        @NotBlank String refreshToken
) {
}
