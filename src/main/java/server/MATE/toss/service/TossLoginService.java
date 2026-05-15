package server.MATE.toss.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.auth.dto.response.TossLoginResponse;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.auth.crypto.TokenEncryptor;
import server.MATE.domain.auth.store.TossTokenStore;
import server.MATE.domain.auth.store.UserRefreshTokenStore;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.domain.users.dto.response.MeResponse;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.client.login.TossLoginApiClient;
import server.MATE.toss.crypto.TossUserInfoDecryptor;
import server.MATE.toss.dto.response.TossDecryptedUserInfo;
import server.MATE.toss.dto.response.TossLoginUserResponse;
import server.MATE.toss.dto.request.TossTokenRequest;
import server.MATE.toss.dto.response.TossTokenResponse;
import server.MATE.toss.exception.TossApiException;
import server.MATE.toss.exception.TossErrorCode;

@Slf4j
@Service
@Transactional
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class TossLoginService {

    private final TossLoginApiClient tossLoginApiClient;
    private final TossUserInfoDecryptor tossUserInfoDecryptor;
    private final UsersRepository usersRepository;
    private final TossAccountRepository tossAccountRepository;
    private final JwtProvider jwtProvider;
    private final UserRefreshTokenStore userRefreshTokenStore;
    private final TossTokenStore tossTokenStore;
    private final TokenEncryptor tokenEncryptor;
    private final Clock clock;

    @Value("${toss.token.refresh-cache-ttl}")
    private long tossRefreshCacheTtlMillis;

    public TossLoginResponse login(String authorizationCode, String referrer) {
        TossTokenResponse tossTokenResponse = tossLoginApiClient.generateToken(new TossTokenRequest(authorizationCode, referrer));
        validateTokenResponse(tossTokenResponse);

        TossLoginUserResponse loginUserResponse = tossLoginApiClient.getLoginUserInfo(tossTokenResponse.accessToken());
        logRawLoginUserResponse(loginUserResponse);
        validateLoginUserResponse(loginUserResponse);
        validateCiScope(loginUserResponse);

        TossDecryptedUserInfo decryptedUserInfo = tossUserInfoDecryptor.decrypt(loginUserResponse);

        UserLoginContext userLoginContext = upsertUser(decryptedUserInfo);
        String encryptedRefreshToken = tokenEncryptor.encrypt(tossTokenResponse.refreshToken());
        syncTossAccount(userLoginContext.user(), decryptedUserInfo, encryptedRefreshToken);
        cacheTossTokens(userLoginContext.user().getId(), tossTokenResponse);

        String accessToken = jwtProvider.generateToken(userLoginContext.user().getId(), userLoginContext.user().getRole().name(), TokenType.ACCESS);
        String refreshToken = jwtProvider.generateToken(userLoginContext.user().getId(), userLoginContext.user().getRole().name(), TokenType.REFRESH);
        persistUserRefreshToken(userLoginContext.user().getId(), refreshToken);

        return new TossLoginResponse(
                accessToken,
                refreshToken,
                MeResponse.from(userLoginContext.user()),
                userLoginContext.isNewUser()
        );
    }

    private UserLoginContext upsertUser(TossDecryptedUserInfo decryptedUserInfo) {
        return usersRepository.findByCi(decryptedUserInfo.ci())
                .map(existingUser -> {
                    existingUser.updateName(decryptedUserInfo.name());
                    return new UserLoginContext(existingUser, false);
                })
                .orElseGet(() -> new UserLoginContext(
                        usersRepository.save(Users.builder()
                                .ci(decryptedUserInfo.ci())
                                .name(decryptedUserInfo.name())
                                .build()),
                        true
                ));
    }

    private void syncTossAccount(
            Users user,
            TossDecryptedUserInfo decryptedUserInfo,
            String encryptedRefreshToken
    ) {
        LocalDateTime now = LocalDateTime.now(clock);

        TossAccount tossAccount = tossAccountRepository.findByUser(user)
                .orElseGet(() -> TossAccount.builder()
                        .user(user)
                        .tossUserKey(decryptedUserInfo.userKey())
                        .lastLoginAt(now)
                        .lastTokenRefreshedAt(now)
                        .build());

        tossAccount.syncLoginState(
                decryptedUserInfo.userKey(),
                encryptedRefreshToken,
                decryptedUserInfo.scope(),
                now,
                now
        );

        tossAccountRepository.save(tossAccount);
    }

    private void cacheTossTokens(Long userId, TossTokenResponse tossTokenResponse) {
        try {
            long expiresIn = tossTokenResponse.expiresIn() == null ? 0L : tossTokenResponse.expiresIn();
            if (expiresIn > 0) tossTokenStore.saveAccessToken(userId, tossTokenResponse.accessToken(), expiresIn * 1000L);

            tossTokenStore.saveRefreshToken(userId, tossTokenResponse.refreshToken(), tossRefreshCacheTtlMillis);
        } catch (Exception e) {
            throw new BaseException(BaseErrorCode.AUTH_009, BaseErrorCode.AUTH_009.getMessage(), e);
        }
    }

    private void persistUserRefreshToken(Long userId, String refreshToken) {
        try {
            userRefreshTokenStore.save(userId, refreshToken, jwtProvider.getExpiration(TokenType.REFRESH));
        } catch (Exception e) {
            throw new BaseException(BaseErrorCode.AUTH_009, BaseErrorCode.AUTH_009.getMessage(), e);
        }
    }

    private void validateTokenResponse(TossTokenResponse tossTokenResponse) {
        if (tossTokenResponse == null || tossTokenResponse.accessToken() == null || tossTokenResponse.refreshToken() == null) {
            throw new TossApiException(TossErrorCode.TOSS_002, "토스 토큰 응답에 필수 값이 누락되었습니다.", null);
        }
    }

    private void validateLoginUserResponse(TossLoginUserResponse loginUserResponse) {
        if (loginUserResponse == null || loginUserResponse.userKey() == null) {
            throw new TossApiException(TossErrorCode.TOSS_007, "토스 사용자 식별 정보가 누락되었습니다.", null);
        }
    }

    private void validateCiScope(TossLoginUserResponse loginUserResponse) {
        String scope = loginUserResponse.scope();
        boolean hasUserCiScope = scope != null
                && Arrays.stream(scope.split(","))
                .map(String::trim)
                .anyMatch("user_ci"::equals);

        if (!hasUserCiScope) {
            throw new BaseException(
                    BaseErrorCode.AUTH_008,
                    "토스 응답에 user_ci scope가 없습니다. 토스 로그인 권한에 CI가 포함되어 있는지, 기존 연결을 해제한 뒤 다시 동의했는지 확인해주세요."
            );
        }
    }

    private void logRawLoginUserResponse(TossLoginUserResponse loginUserResponse) {
        if (loginUserResponse == null) {
            log.debug("Toss login-me raw response is null");
            return;
        }

        log.debug(
                "Toss login-me raw response: userKey={}, scope='{}', ciPresent={}, ciLength={}, namePresent={}, nameLength={}",
                loginUserResponse.userKey(),
                loginUserResponse.scope(),
                hasText(loginUserResponse.ci()),
                safeLength(loginUserResponse.ci()),
                hasText(loginUserResponse.name()),
                safeLength(loginUserResponse.name())
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Integer safeLength(String value) {
        return value == null ? null : value.length();
    }

    private record UserLoginContext(Users user, boolean isNewUser) {
    }
}
