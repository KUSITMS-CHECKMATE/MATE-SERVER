package server.MATE.domain.auth.dto.response;

public record TestTokenResponse(
        Long userId,
        String role,
        String accessToken,
        String refreshToken
) {
}
