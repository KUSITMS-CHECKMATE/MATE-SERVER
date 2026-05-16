package server.MATE.domain.auth.dto.response;

public record AuthReissueResponse(
        String accessToken,
        String refreshToken
) {
}
