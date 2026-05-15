package server.MATE.toss.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import server.MATE.domain.auth.dto.response.TossLoginResponse;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.auth.crypto.TokenEncryptor;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.store.UserRefreshTokenStore;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.dto.response.MeResponse;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.client.login.TossLoginApiClient;
import server.MATE.domain.users.entity.TossUnlinkReferrer;
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
    private final TokenEncryptor tokenEncryptor;
    private final TossLoginSessionService tossLoginSessionService;
    private final Clock clock;

    @Value("${toss.callback.basic-auth-header:}")
    private String callbackBasicAuthHeader;

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
        tossLoginSessionService.persistLoginTokens(userLoginContext.user().getId(), tossTokenResponse);

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

    public void unlinkCurrentUser(Long userId) {
        Optional<TossAccount> tossAccountOptional = tossAccountRepository.findByUserId(userId);
        if (tossAccountOptional.isEmpty()) {
            log.warn("TOSS_UNLINK linked account not found. userId={}", userId);
            return;
        }

        TossAccount tossAccount = tossAccountOptional.get();
        if (!tossAccount.isLinked()) {
            log.info("TOSS_UNLINK already unlinked. userId={}, tossUserKey={}", userId, tossAccount.getTossUserKey());
            return;
        }

        Optional<String> accessTokenOptional = tossLoginSessionService.getValidAccessToken(userId);
        if (accessTokenOptional.isPresent()) {
            try {
                removeByAccessTokenWithRetry(userId, accessTokenOptional.get());
            } catch (BaseException e) {
                if (!isIgnorableUnlinkException(e)) {
                    throw e;
                }
                log.warn("TOSS_UNLINK idempotent success after token validation failure. userId={}, tossUserKey={}, code={}",
                        userId, tossAccount.getTossUserKey(), e.getErrorCode().getCode());
            }
        } else {
            log.warn("TOSS_UNLINK idempotent success: no toss access/refresh token. userId={}, tossUserKey={}",
                    userId, tossAccount.getTossUserKey());
        }

        revokeLinkedState(tossAccount.getUser(), tossAccount, TossUnlinkReferrer.UNLINK);
    }

    public void unlinkByUserKey(Long userKey, TossUnlinkReferrer referrer) {
        TossAccount tossAccount = tossAccountRepository.findByTossUserKey(userKey)
                .orElse(null);
        if (tossAccount == null) {
            log.warn("TOSS_UNLINK_BY_USER_KEY linked account not found. userKey={}", userKey);
            return;
        }

        if (!tossAccount.isLinked()) {
            log.info("TOSS_UNLINK_BY_USER_KEY already unlinked. userId={}, tossUserKey={}",
                    tossAccount.getUser().getId(), userKey);
            return;
        }

        try {
            tossLoginApiClient.removeByUserKey(userKey);
        } catch (TossApiException e) {
            if (!isIgnorableUnlinkException(e)) {
                throw e;
            }
            log.warn("TOSS_UNLINK_BY_USER_KEY idempotent success after remote not found. userId={}, tossUserKey={}, code={}",
                    tossAccount.getUser().getId(), userKey, e.getErrorCode().getCode());
        }
        revokeLinkedState(tossAccount.getUser(), tossAccount, referrer);
    }

    public void handleUnlinkCallback(Long userKey, TossUnlinkReferrer referrer) {
        TossAccount tossAccount = tossAccountRepository.findByTossUserKey(userKey)
                .orElse(null);
        if (tossAccount == null) {
            log.warn("TOSS_CALLBACK linked account not found. userKey={}, referrer={}", userKey, referrer);
            return;
        }

        if (!tossAccount.isLinked()) {
            log.info("TOSS_CALLBACK already unlinked. userId={}, tossUserKey={}, referrer={}",
                    tossAccount.getUser().getId(), userKey, referrer);
            return;
        }

        revokeLinkedState(tossAccount.getUser(), tossAccount, referrer);
    }

    public boolean isLinked(Long userId) {
        return tossAccountRepository.findByUserId(userId)
                .map(TossAccount::isLinked)
                .orElse(false);
    }

    public void validateCallbackAuthorization(String authorization) {
        String expectedHeader = callbackBasicAuthHeader == null ? "" : callbackBasicAuthHeader.trim();
        if (expectedHeader.isBlank() || authorization == null || !authorization.startsWith("Basic ")) {
            throw new BaseException(BaseErrorCode.COMMON_008, "유효하지 않은 토스 연결 해제 콜백 인증입니다.");
        }

        try {
            String encodedCredentials = authorization.substring("Basic ".length()).trim();
            byte[] decodedBytes = Base64.getDecoder().decode(encodedCredentials);
            byte[] expectedBytes = expectedHeader.getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expectedBytes, decodedBytes)) {
                throw new BaseException(BaseErrorCode.COMMON_008, "유효하지 않은 토스 연결 해제 콜백 인증입니다.");
            }
        } catch (IllegalArgumentException e) {
            throw new BaseException(BaseErrorCode.COMMON_008, "유효하지 않은 토스 연결 해제 콜백 인증입니다.");
        }
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

    private void removeByAccessTokenWithRetry(Long userId, String accessToken) {
        try {
            tossLoginApiClient.removeByAccessToken(accessToken);
        } catch (TossApiException e) {
            if (!isInvalidAccessToken(e)) {
                throw e;
            }

            TossTokenResponse refreshed = tossLoginSessionService.refreshLoginTokens(userId);
            tossLoginApiClient.removeByAccessToken(refreshed.accessToken());
        }
    }

    private void revokeLinkedState(Users user, TossAccount tossAccount, TossUnlinkReferrer referrer) {
        tossLoginSessionService.clearLoginTokens(user.getId());
        userRefreshTokenStore.delete(user.getId());
        tossAccount.markUnlinked(referrer, LocalDateTime.now(clock));
    }

    private boolean isInvalidAccessToken(TossApiException e) {
        return e.getErrorCode() == TossErrorCode.TOSS_005;
    }

    private boolean isIgnorableUnlinkException(BaseException e) {
        return e.getErrorCode() == BaseErrorCode.AUTH_013
                || e.getErrorCode() == TossErrorCode.TOSS_004
                || e.getErrorCode() == TossErrorCode.TOSS_005
                || e.getErrorCode() == TossErrorCode.TOSS_007
                || e.getErrorCode() == TossErrorCode.TOSS_008;
    }

    private record UserLoginContext(Users user, boolean isNewUser) {
    }
}
