package server.MATE.domain.auth.dto.response;

import server.MATE.domain.users.dto.response.MeResponse;

public record TossLoginResponse(
        String accessToken,
        String refreshToken,
        MeResponse meResponse,
        boolean isNewUser
) {
}
