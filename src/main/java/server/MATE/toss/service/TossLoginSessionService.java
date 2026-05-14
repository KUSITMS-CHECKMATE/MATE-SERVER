package server.MATE.toss.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.auth.crypto.TokenEncryptor;
import server.MATE.domain.auth.store.TossTokenStore;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.client.login.TossLoginApiClient;
import server.MATE.toss.dto.request.TossRefreshTokenRequest;
import server.MATE.toss.dto.response.TossTokenResponse;

@Service
@Transactional
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
public class TossLoginSessionService {

    private final TossLoginApiClient tossLoginApiClient;
    private final TossAccountRepository tossAccountRepository;
    private final TossTokenStore tossTokenStore;
    private final TokenEncryptor tokenEncryptor;
    private final Clock clock;

    @Value("${toss.token.refresh-cache-ttl}")
    private long tossRefreshCacheTtlMillis;

    public Optional<String> getValidAccessToken(Long userId) {
        Optional<String> cachedAccessToken = tossTokenStore.findAccessToken(userId);
        if (cachedAccessToken.isPresent()) return cachedAccessToken;

        return findRefreshToken(userId)
                .map(refreshToken -> refreshLoginTokens(userId, refreshToken).accessToken());
    }

    public TossTokenResponse refreshLoginTokens(Long userId) {
        String refreshToken = findRefreshToken(userId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.AUTH_013, "토스 refresh token을 찾을 수 없습니다."));
        return refreshLoginTokens(userId, refreshToken);
    }

    public void clearLoginTokens(Long userId) {
        tossTokenStore.deleteAccessToken(userId);
        tossTokenStore.deleteRefreshToken(userId);
    }

    private TossTokenResponse refreshLoginTokens(Long userId, String refreshToken) {
        TossTokenResponse response = tossLoginApiClient.refreshToken(new TossRefreshTokenRequest(refreshToken));
        persistLoginTokens(userId, response);
        return response;
    }

    public void persistLoginTokens(Long userId, TossTokenResponse response) {
        validateTokenResponse(response);

        try {
            long expiresInMillis = response.expiresIn() == null ? 0L : response.expiresIn() * 1000L;
            if (expiresInMillis > 0) tossTokenStore.saveAccessToken(userId, response.accessToken(), expiresInMillis);

            tossTokenStore.saveRefreshToken(userId, response.refreshToken(), tossRefreshCacheTtlMillis);

            TossAccount tossAccount = tossAccountRepository.findByUserId(userId)
                    .orElseThrow(() -> new BaseException(BaseErrorCode.AUTH_004, "토스 연동 계정을 찾을 수 없습니다."));
            tossAccount.refreshLoginState(
                    tokenEncryptor.encrypt(response.refreshToken()),
                    response.scope(),
                    LocalDateTime.now(clock)
            );
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(BaseErrorCode.AUTH_009, BaseErrorCode.AUTH_009.getMessage(), e);
        }
    }

    private Optional<String> findRefreshToken(Long userId) {
        Optional<String> cachedRefreshToken = tossTokenStore.findRefreshToken(userId);
        if (cachedRefreshToken.isPresent()) return cachedRefreshToken;

        return tossAccountRepository.findByUserId(userId)
                .map(TossAccount::getEncryptedTossRefreshToken)
                .filter(value -> value != null && !value.isBlank())
                .map(tokenEncryptor::decrypt);
    }

    private void validateTokenResponse(TossTokenResponse response) {
        if (response == null || response.accessToken() == null || response.refreshToken() == null) {
            throw new BaseException(BaseErrorCode.AUTH_008, "토스 토큰 재발급 응답에 필수 값이 누락되었습니다.");
        }
    }
}
