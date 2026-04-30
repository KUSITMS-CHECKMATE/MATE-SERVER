package server.MATE.toss.dto;

import jakarta.validation.constraints.NotBlank;

public record TossRefreshTokenRequest(
        @NotBlank String refreshToken
) {
}
