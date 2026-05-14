package server.MATE.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AuthReissueRequest(
        @NotBlank String refreshToken
) {
}
