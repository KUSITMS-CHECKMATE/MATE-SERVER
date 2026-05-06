package server.MATE.domain.auth.dto.response;

public record TestTokenResponse(
        String accessToken,
        String tokenType
) {
}
