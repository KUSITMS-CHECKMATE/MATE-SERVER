package server.MATE.toss.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossTokenResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        Long expiresIn,
        String scope
) {
}
