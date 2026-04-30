package server.MATE.toss.client.login;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.dto.TossRefreshTokenRequest;
import server.MATE.toss.dto.TossTokenRequest;
import server.MATE.toss.dto.TossTokenResponse;

@Component
@ConditionalOnBean(name = "tossWebClient")
@RequiredArgsConstructor
public class TossLoginApiClient {

    private final TossHttpClient tossHttpClient;

    public TossTokenResponse generateToken(TossTokenRequest request) {
        return tossHttpClient.post(
                "/api-partner/v1/apps-in-toss/user/oauth2/generate-token",
                request,
                TossTokenResponse.class
        );
    }

    public TossTokenResponse refreshToken(TossRefreshTokenRequest request) {
        return tossHttpClient.post(
                "/api-partner/v1/apps-in-toss/user/oauth2/refresh-token",
                request,
                TossTokenResponse.class
        );
    }
}
