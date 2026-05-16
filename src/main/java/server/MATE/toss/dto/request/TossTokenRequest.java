package server.MATE.toss.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TossTokenRequest(
        @NotBlank String authorizationCode,
        @NotBlank String referrer
) {
}
