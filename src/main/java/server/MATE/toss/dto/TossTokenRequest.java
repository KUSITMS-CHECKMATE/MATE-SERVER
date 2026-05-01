package server.MATE.toss.dto;

import jakarta.validation.constraints.NotBlank;

public record TossTokenRequest(
        @NotBlank String authorizationCode,
        @NotBlank String referrer
) {
}
