package server.MATE.toss.client.login;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import server.MATE.toss.dto.request.TossUnlinkByUserKeyRequest;
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.dto.response.TossLoginUserResponse;
import server.MATE.toss.dto.request.TossRefreshTokenRequest;
import server.MATE.toss.dto.request.TossTokenRequest;
import server.MATE.toss.dto.response.TossTokenResponse;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
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

    public TossLoginUserResponse getLoginUserInfo(String accessToken) {
        return tossHttpClient.get(
                "/api-partner/v1/apps-in-toss/user/oauth2/login-me",
                headers -> headers.setBearerAuth(accessToken),
                TossLoginUserResponse.class
        );
    }

    public void removeByAccessToken(String accessToken) {
        tossHttpClient.post(
                "/api-partner/v1/apps-in-toss/user/oauth2/access/remove-by-access-token",
                Map.of(),
                headers -> headers.setBearerAuth(accessToken),
                Object.class
        );
    }

    public void removeByUserKey(Long userKey) {
        tossHttpClient.post(
                "/api-partner/v1/apps-in-toss/user/oauth2/access/remove-by-user-key",
                new TossUnlinkByUserKeyRequest(userKey),
                Object.class
        );
    }
}
