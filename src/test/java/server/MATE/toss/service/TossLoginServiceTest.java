package server.MATE.toss.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import server.MATE.domain.auth.crypto.TokenEncryptor;
import server.MATE.domain.auth.dto.response.TossLoginResponse;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.auth.store.UserRefreshTokenStore;
import server.MATE.domain.users.entity.Role;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.entity.TossUnlinkReferrer;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.client.login.TossLoginApiClient;
import server.MATE.toss.crypto.TossUserInfoDecryptor;
import server.MATE.toss.dto.request.TossTokenRequest;
import server.MATE.toss.dto.response.TossDecryptedUserInfo;
import server.MATE.toss.dto.response.TossLoginUserResponse;
import server.MATE.toss.dto.response.TossTokenResponse;
import server.MATE.toss.exception.TossApiException;
import server.MATE.toss.exception.TossErrorCode;

@ExtendWith(MockitoExtension.class)
class TossLoginServiceTest {

    @Mock
    private TossLoginApiClient tossLoginApiClient;

    @Mock
    private TossUserInfoDecryptor tossUserInfoDecryptor;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private TossAccountRepository tossAccountRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRefreshTokenStore userRefreshTokenStore;

    @Mock
    private TokenEncryptor tokenEncryptor;

    @Mock
    private TossLoginSessionService tossLoginSessionService;

    private TossLoginService tossLoginService;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @BeforeEach
    void setUp() {
        tossLoginService = new TossLoginService(
                tossLoginApiClient,
                tossUserInfoDecryptor,
                usersRepository,
                tossAccountRepository,
                jwtProvider,
                userRefreshTokenStore,
                tokenEncryptor,
                tossLoginSessionService,
                fixedClock
        );
        ReflectionTestUtils.setField(tossLoginService, "callbackBasicAuthHeader", "callback-secret");
    }

    @Nested
    class LoginTest {

        @Test
        @DisplayName("신규 사용자 로그인 성공 시 사용자 생성, TossAccount 동기화, Mate 토큰 발급이 수행된다")
        void logsInNewUserSuccessfully() {
            TossTokenResponse tokenResponse = new TossTokenResponse("Bearer", "toss-access", "toss-refresh", 3600L, "user_ci");
            TossLoginUserResponse loginUserResponse = new TossLoginUserResponse(777L, "user_ci,name", "encrypted-name", "encrypted-ci");
            TossDecryptedUserInfo decryptedUserInfo = new TossDecryptedUserInfo(777L, "user_ci,name", "ci-1", "new-user");
            Users savedUser = createUser(1L, "ci-1", "new-user");

            when(tossLoginApiClient.generateToken(new TossTokenRequest("authorization-code", "APP"))).thenReturn(tokenResponse);
            when(tossLoginApiClient.getLoginUserInfo("toss-access")).thenReturn(loginUserResponse);
            when(tossUserInfoDecryptor.decrypt(loginUserResponse)).thenReturn(decryptedUserInfo);
            when(usersRepository.findByCi("ci-1")).thenReturn(Optional.empty());
            when(usersRepository.save(any(Users.class))).thenReturn(savedUser);
            when(tokenEncryptor.encrypt("toss-refresh")).thenReturn("encrypted-refresh");
            when(tossAccountRepository.findByUser(savedUser)).thenReturn(Optional.empty());
            when(jwtProvider.generateToken(1L, "USER", TokenType.ACCESS)).thenReturn("mate-access");
            when(jwtProvider.generateToken(1L, "USER", TokenType.REFRESH)).thenReturn("mate-refresh");
            when(jwtProvider.getExpiration(TokenType.REFRESH)).thenReturn(1_209_600_000L);

            TossLoginResponse response = tossLoginService.login("authorization-code", "APP");

            assertThat(response.accessToken()).isEqualTo("mate-access");
            assertThat(response.refreshToken()).isEqualTo("mate-refresh");
            assertThat(response.meResponse().id()).isEqualTo(1L);
            assertThat(response.isNewUser()).isTrue();
            verify(tossLoginSessionService).persistLoginTokens(1L, tokenResponse);
            verify(userRefreshTokenStore).save(1L, "mate-refresh", 1_209_600_000L);

            ArgumentCaptor<TossAccount> tossAccountCaptor = ArgumentCaptor.forClass(TossAccount.class);
            verify(tossAccountRepository).save(tossAccountCaptor.capture());
            TossAccount savedAccount = tossAccountCaptor.getValue();
            assertThat(savedAccount.getTossUserKey()).isEqualTo(777L);
            assertThat(savedAccount.getEncryptedTossRefreshToken()).isEqualTo("encrypted-refresh");
            assertThat(savedAccount.getScope()).isEqualTo("user_ci,name");
            assertThat(savedAccount.isLinked()).isTrue();
        }

        @Test
        @DisplayName("기존 사용자 로그인 성공 시 이름을 갱신하고 linked 상태를 복구한다")
        void logsInExistingUserSuccessfully() {
            TossTokenResponse tokenResponse = new TossTokenResponse("Bearer", "toss-access", "toss-refresh", 3600L, "user_ci");
            TossLoginUserResponse loginUserResponse = new TossLoginUserResponse(777L, "user_ci,name", "encrypted-name", "encrypted-ci");
            TossDecryptedUserInfo decryptedUserInfo = new TossDecryptedUserInfo(777L, "user_ci,name", "ci-1", "renamed-user");
            Users existingUser = createUser(1L, "ci-1", "before-name");
            TossAccount existingAccount = TossAccount.builder()
                    .user(existingUser)
                    .tossUserKey(123L)
                    .encryptedTossRefreshToken("old-refresh")
                    .scope("old")
                    .isLinked(false)
                    .unlinkReferrer(TossUnlinkReferrer.UNLINK)
                    .unlinkedAt(LocalDateTime.of(2026, 5, 14, 10, 0))
                    .lastLoginAt(LocalDateTime.of(2026, 5, 14, 10, 0))
                    .lastTokenRefreshedAt(LocalDateTime.of(2026, 5, 14, 10, 0))
                    .build();

            when(tossLoginApiClient.generateToken(new TossTokenRequest("authorization-code", "APP"))).thenReturn(tokenResponse);
            when(tossLoginApiClient.getLoginUserInfo("toss-access")).thenReturn(loginUserResponse);
            when(tossUserInfoDecryptor.decrypt(loginUserResponse)).thenReturn(decryptedUserInfo);
            when(usersRepository.findByCi("ci-1")).thenReturn(Optional.of(existingUser));
            when(tokenEncryptor.encrypt("toss-refresh")).thenReturn("encrypted-refresh");
            when(tossAccountRepository.findByUser(existingUser)).thenReturn(Optional.of(existingAccount));
            when(jwtProvider.generateToken(1L, "USER", TokenType.ACCESS)).thenReturn("mate-access");
            when(jwtProvider.generateToken(1L, "USER", TokenType.REFRESH)).thenReturn("mate-refresh");
            when(jwtProvider.getExpiration(TokenType.REFRESH)).thenReturn(1_209_600_000L);

            TossLoginResponse response = tossLoginService.login("authorization-code", "APP");

            assertThat(response.isNewUser()).isFalse();
            assertThat(existingUser.getName()).isEqualTo("renamed-user");
            assertThat(existingAccount.getTossUserKey()).isEqualTo(777L);
            assertThat(existingAccount.isLinked()).isTrue();
            assertThat(existingAccount.getUnlinkReferrer()).isNull();
            assertThat(existingAccount.getUnlinkedAt()).isNull();
            verify(usersRepository, never()).save(any());
        }

        @Test
        @DisplayName("토큰 응답 필수값이 누락되면 TOSS_002 예외를 던진다")
        void throwsWhenTokenResponseMissingRequiredFields() {
            when(tossLoginApiClient.generateToken(new TossTokenRequest("authorization-code", "APP")))
                    .thenReturn(new TossTokenResponse("Bearer", null, "toss-refresh", 3600L, "user_ci"));

            assertThatThrownBy(() -> tossLoginService.login("authorization-code", "APP"))
                    .isInstanceOf(TossApiException.class)
                    .extracting(exception -> ((TossApiException) exception).getErrorCode())
                    .isEqualTo(TossErrorCode.TOSS_002);
        }

        @Test
        @DisplayName("login-me 응답에 userKey가 없으면 TOSS_007 예외를 던진다")
        void throwsWhenUserKeyMissingInLoginMeResponse() {
            TossTokenResponse tokenResponse = new TossTokenResponse("Bearer", "toss-access", "toss-refresh", 3600L, "user_ci");
            when(tossLoginApiClient.generateToken(new TossTokenRequest("authorization-code", "APP"))).thenReturn(tokenResponse);
            when(tossLoginApiClient.getLoginUserInfo("toss-access"))
                    .thenReturn(new TossLoginUserResponse(null, "user_ci", "encrypted-name", "encrypted-ci"));

            assertThatThrownBy(() -> tossLoginService.login("authorization-code", "APP"))
                    .isInstanceOf(TossApiException.class)
                    .extracting(exception -> ((TossApiException) exception).getErrorCode())
                    .isEqualTo(TossErrorCode.TOSS_007);
        }

        @Test
        @DisplayName("user_ci scope가 없으면 AUTH_008 예외를 던진다")
        void throwsWhenUserCiScopeMissing() {
            TossTokenResponse tokenResponse = new TossTokenResponse("Bearer", "toss-access", "toss-refresh", 3600L, "name");
            when(tossLoginApiClient.generateToken(new TossTokenRequest("authorization-code", "APP"))).thenReturn(tokenResponse);
            when(tossLoginApiClient.getLoginUserInfo("toss-access"))
                    .thenReturn(new TossLoginUserResponse(777L, "name", "encrypted-name", "encrypted-ci"));

            assertThatThrownBy(() -> tossLoginService.login("authorization-code", "APP"))
                    .isInstanceOf(BaseException.class)
                    .extracting(exception -> ((BaseException) exception).getErrorCode())
                    .isEqualTo(BaseErrorCode.AUTH_008);
        }

        @Test
        @DisplayName("토스 로그인 중 네트워크 실패가 발생하면 TOSS_001 예외를 그대로 전파한다")
        void propagatesToss001WhenGenerateTokenFailsByNetwork() {
            when(tossLoginApiClient.generateToken(new TossTokenRequest("authorization-code", "APP")))
                    .thenThrow(new TossApiException(TossErrorCode.TOSS_001, "토스 API 호출에 실패했습니다.", null));

            assertThatThrownBy(() -> tossLoginService.login("authorization-code", "APP"))
                    .isInstanceOf(TossApiException.class)
                    .extracting(exception -> ((TossApiException) exception).getErrorCode())
                    .isEqualTo(TossErrorCode.TOSS_001);
        }
    }

    @Nested
    class UnlinkTest {

        @Test
        @DisplayName("linked account가 없으면 조용히 종료한다")
        void returnsWhenLinkedAccountDoesNotExist() {
            when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.empty());

            tossLoginService.unlinkCurrentUser(1L);

            verify(tossLoginSessionService, never()).getValidAccessToken(any());
            verify(userRefreshTokenStore, never()).delete(any());
        }

        @Test
        @DisplayName("이미 unlink 상태면 조용히 종료한다")
        void returnsWhenAlreadyUnlinked() {
            TossAccount tossAccount = createUnlinkedAccount(1L);
            when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.of(tossAccount));

            tossLoginService.unlinkCurrentUser(1L);

            verify(tossLoginSessionService, never()).getValidAccessToken(any());
            verify(userRefreshTokenStore, never()).delete(any());
        }

        @Test
        @DisplayName("access token이 있으면 remove-by-access-token을 수행하고 revoke 한다")
        void unlinksCurrentUserWithAccessToken() {
            TossAccount tossAccount = createLinkedAccount(1L);
            when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.of(tossAccount));
            when(tossLoginSessionService.getValidAccessToken(1L)).thenReturn(Optional.of("valid-access"));

            tossLoginService.unlinkCurrentUser(1L);

            verify(tossLoginApiClient).removeByAccessToken("valid-access");
            verify(tossLoginSessionService).clearLoginTokens(1L);
            verify(userRefreshTokenStore).delete(1L);
            assertThat(tossAccount.isLinked()).isFalse();
            assertThat(tossAccount.getUnlinkReferrer()).isEqualTo(TossUnlinkReferrer.UNLINK);
            assertThat(tossAccount.getUnlinkedAt()).isEqualTo(LocalDateTime.now(fixedClock));
        }

        @Test
        @DisplayName("access token이 없고 refresh 가능하면 재발급된 access token으로 unlink 한다")
        void unlinksCurrentUserWhenRefreshedAccessTokenAvailable() {
            TossAccount tossAccount = createLinkedAccount(1L);
            when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.of(tossAccount));
            when(tossLoginSessionService.getValidAccessToken(1L)).thenReturn(Optional.of("refreshed-access"));

            tossLoginService.unlinkCurrentUser(1L);

            verify(tossLoginApiClient).removeByAccessToken("refreshed-access");
            verify(tossLoginSessionService).clearLoginTokens(1L);
            verify(userRefreshTokenStore).delete(1L);
            assertThat(tossAccount.isLinked()).isFalse();
        }

        @Test
        @DisplayName("access token과 refresh token이 모두 없으면 멱등 성공으로 처리하고 revoke 한다")
        void treatsMissingTokensAsIdempotentSuccess() {
            TossAccount tossAccount = createLinkedAccount(1L);
            when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.of(tossAccount));
            when(tossLoginSessionService.getValidAccessToken(1L)).thenReturn(Optional.empty());

            tossLoginService.unlinkCurrentUser(1L);

            verify(tossLoginApiClient, never()).removeByAccessToken(any());
            verify(tossLoginSessionService).clearLoginTokens(1L);
            verify(userRefreshTokenStore).delete(1L);
            assertThat(tossAccount.isLinked()).isFalse();
        }

        @Test
        @DisplayName("invalid access token이면 refresh 후 재시도한다")
        void retriesAfterRefreshingWhenAccessTokenInvalid() {
            TossAccount tossAccount = createLinkedAccount(1L);
            TossTokenResponse refreshed = new TossTokenResponse("Bearer", "new-access", "new-refresh", 3600L, "user_ci");

            when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.of(tossAccount));
            when(tossLoginSessionService.getValidAccessToken(1L)).thenReturn(Optional.of("old-access"));
            doThrow(new TossApiException(TossErrorCode.TOSS_005, "invalid access token", null))
                    .when(tossLoginApiClient).removeByAccessToken("old-access");
            when(tossLoginSessionService.refreshLoginTokens(1L)).thenReturn(refreshed);

            tossLoginService.unlinkCurrentUser(1L);

            verify(tossLoginApiClient).removeByAccessToken("old-access");
            verify(tossLoginSessionService).refreshLoginTokens(1L);
            verify(tossLoginApiClient).removeByAccessToken("new-access");
            verify(userRefreshTokenStore).delete(1L);
        }

        @Test
        @DisplayName("ignorable unlink 예외는 삼키고 revoke는 수행한다")
        void ignoresIgnorableUnlinkExceptionAndStillRevokes() {
            TossAccount tossAccount = createLinkedAccount(1L);
            when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.of(tossAccount));
            when(tossLoginSessionService.getValidAccessToken(1L)).thenReturn(Optional.of("valid-access"));
            doThrow(new TossApiException(TossErrorCode.TOSS_007, "user key not found", null))
                    .when(tossLoginApiClient).removeByAccessToken("valid-access");

            tossLoginService.unlinkCurrentUser(1L);

            verify(tossLoginSessionService).clearLoginTokens(1L);
            verify(userRefreshTokenStore).delete(1L);
            assertThat(tossAccount.isLinked()).isFalse();
        }

        @Test
        @DisplayName("unlink 중 네트워크 실패가 발생하면 TOSS_001 예외를 전파하고 revoke 하지 않는다")
        void throwsToss001AndDoesNotRevokeWhenNetworkFailureOccursDuringUnlink() {
            TossAccount tossAccount = createLinkedAccount(1L);
            when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.of(tossAccount));
            when(tossLoginSessionService.getValidAccessToken(1L)).thenReturn(Optional.of("valid-access"));
            doThrow(new TossApiException(TossErrorCode.TOSS_001, "토스 API 호출에 실패했습니다.", null))
                    .when(tossLoginApiClient).removeByAccessToken("valid-access");

            assertThatThrownBy(() -> tossLoginService.unlinkCurrentUser(1L))
                    .isInstanceOf(TossApiException.class)
                    .extracting(exception -> ((TossApiException) exception).getErrorCode())
                    .isEqualTo(TossErrorCode.TOSS_001);

            verify(tossLoginSessionService, never()).clearLoginTokens(1L);
            verify(userRefreshTokenStore, never()).delete(1L);
            assertThat(tossAccount.isLinked()).isTrue();
        }

        @Test
        @DisplayName("unlinkByUserKey 호출 시 remote not found는 멱등 성공으로 처리한다")
        void treatsRemoteNotFoundAsIdempotentSuccessWhenUnlinkByUserKey() {
            TossAccount tossAccount = createLinkedAccount(1L);
            when(tossAccountRepository.findByTossUserKey(777L)).thenReturn(Optional.of(tossAccount));
            doThrow(new TossApiException(TossErrorCode.TOSS_007, "not found", null))
                    .when(tossLoginApiClient).removeByUserKey(777L);

            tossLoginService.unlinkByUserKey(777L, TossUnlinkReferrer.UNLINK);

            verify(tossLoginSessionService).clearLoginTokens(1L);
            verify(userRefreshTokenStore).delete(1L);
            assertThat(tossAccount.isLinked()).isFalse();
        }

        @Test
        @DisplayName("unlinkByUserKey 호출 시 정상 unlink를 수행한다")
        void unlinksByUserKeySuccessfully() {
            TossAccount tossAccount = createLinkedAccount(1L);
            when(tossAccountRepository.findByTossUserKey(777L)).thenReturn(Optional.of(tossAccount));

            tossLoginService.unlinkByUserKey(777L, TossUnlinkReferrer.UNLINK);

            verify(tossLoginApiClient).removeByUserKey(777L);
            verify(tossLoginSessionService).clearLoginTokens(1L);
            verify(userRefreshTokenStore).delete(1L);
            assertThat(tossAccount.isLinked()).isFalse();
            assertThat(tossAccount.getUnlinkReferrer()).isEqualTo(TossUnlinkReferrer.UNLINK);
        }

        @Test
        @DisplayName("unlinkByUserKey 호출 시 account가 없으면 조용히 종료한다")
        void returnsWhenUnlinkByUserKeyAccountDoesNotExist() {
            when(tossAccountRepository.findByTossUserKey(777L)).thenReturn(Optional.empty());

            tossLoginService.unlinkByUserKey(777L, TossUnlinkReferrer.UNLINK);

            verify(tossLoginApiClient, never()).removeByUserKey(any());
            verify(tossLoginSessionService, never()).clearLoginTokens(any());
            verify(userRefreshTokenStore, never()).delete(any());
        }

        @Test
        @DisplayName("unlinkByUserKey 호출 시 이미 unlink 상태면 조용히 종료한다")
        void returnsWhenUnlinkByUserKeyAlreadyUnlinked() {
            TossAccount tossAccount = createUnlinkedAccount(1L);
            when(tossAccountRepository.findByTossUserKey(777L)).thenReturn(Optional.of(tossAccount));

            tossLoginService.unlinkByUserKey(777L, TossUnlinkReferrer.UNLINK);

            verify(tossLoginApiClient, never()).removeByUserKey(any());
            verify(tossLoginSessionService, never()).clearLoginTokens(any());
            verify(userRefreshTokenStore, never()).delete(any());
        }

        @Test
        @DisplayName("callback 처리 시 unlink 상태와 refresh token 정리가 수행된다")
        void handlesUnlinkCallbackSuccessfully() {
            TossAccount tossAccount = createLinkedAccount(1L);
            when(tossAccountRepository.findByTossUserKey(777L)).thenReturn(Optional.of(tossAccount));

            tossLoginService.handleUnlinkCallback(777L, TossUnlinkReferrer.WITHDRAWAL_TOSS);

            verify(tossLoginSessionService).clearLoginTokens(1L);
            verify(userRefreshTokenStore).delete(1L);
            assertThat(tossAccount.isLinked()).isFalse();
            assertThat(tossAccount.getUnlinkReferrer()).isEqualTo(TossUnlinkReferrer.WITHDRAWAL_TOSS);
            assertThat(tossAccount.getUnlinkedAt()).isEqualTo(LocalDateTime.now(fixedClock));
        }

        @Test
        @DisplayName("callback 처리 시 account가 없으면 조용히 종료한다")
        void returnsWhenCallbackAccountDoesNotExist() {
            when(tossAccountRepository.findByTossUserKey(777L)).thenReturn(Optional.empty());

            tossLoginService.handleUnlinkCallback(777L, TossUnlinkReferrer.WITHDRAWAL_TOSS);

            verify(tossLoginSessionService, never()).clearLoginTokens(any());
            verify(userRefreshTokenStore, never()).delete(any());
        }

        @Test
        @DisplayName("callback 처리 시 이미 unlink 상태면 조용히 종료한다")
        void returnsWhenCallbackAccountAlreadyUnlinked() {
            TossAccount tossAccount = createUnlinkedAccount(1L);
            when(tossAccountRepository.findByTossUserKey(777L)).thenReturn(Optional.of(tossAccount));

            tossLoginService.handleUnlinkCallback(777L, TossUnlinkReferrer.WITHDRAWAL_TOSS);

            verify(tossLoginSessionService, never()).clearLoginTokens(any());
            verify(userRefreshTokenStore, never()).delete(any());
        }
    }

    @Nested
    class CallbackAuthTest {

        @Test
        @DisplayName("callback auth 검증 성공 케이스를 통과시킨다")
        void validatesCallbackAuthorizationSuccessfully() {
            String header = basicAuth("callback-secret");

            tossLoginService.validateCallbackAuthorization(header);
        }

        @Test
        @DisplayName("callback auth 검증 시 header가 없으면 COMMON_008 예외를 던진다")
        void throwsWhenAuthorizationHeaderMissing() {
            assertThatThrownBy(() -> tossLoginService.validateCallbackAuthorization(null))
                    .isInstanceOf(BaseException.class)
                    .extracting(exception -> ((BaseException) exception).getErrorCode())
                    .isEqualTo(BaseErrorCode.COMMON_008);
        }

        @Test
        @DisplayName("callback auth 검증 시 prefix가 다르면 COMMON_008 예외를 던진다")
        void throwsWhenAuthorizationPrefixInvalid() {
            assertThatThrownBy(() -> tossLoginService.validateCallbackAuthorization("Bearer token"))
                    .isInstanceOf(BaseException.class)
                    .extracting(exception -> ((BaseException) exception).getErrorCode())
                    .isEqualTo(BaseErrorCode.COMMON_008);
        }

        @Test
        @DisplayName("callback auth 검증 시 base64 decode가 실패하면 COMMON_008 예외를 던진다")
        void throwsWhenAuthorizationIsNotBase64() {
            assertThatThrownBy(() -> tossLoginService.validateCallbackAuthorization("Basic !!!"))
                    .isInstanceOf(BaseException.class)
                    .extracting(exception -> ((BaseException) exception).getErrorCode())
                    .isEqualTo(BaseErrorCode.COMMON_008);
        }

        @Test
        @DisplayName("callback auth 검증 시 값이 다르면 COMMON_008 예외를 던진다")
        void throwsWhenAuthorizationValueDoesNotMatch() {
            assertThatThrownBy(() -> tossLoginService.validateCallbackAuthorization(basicAuth("wrong-secret")))
                    .isInstanceOf(BaseException.class)
                    .extracting(exception -> ((BaseException) exception).getErrorCode())
                    .isEqualTo(BaseErrorCode.COMMON_008);
        }
    }

    @Test
    @DisplayName("isLinked는 연동 상태를 올바르게 반환한다")
    void returnsLinkedStatus() {
        when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.of(createLinkedAccount(1L)));
        when(tossAccountRepository.findByUserId(2L)).thenReturn(Optional.of(createUnlinkedAccount(2L)));
        when(tossAccountRepository.findByUserId(3L)).thenReturn(Optional.empty());

        assertThat(tossLoginService.isLinked(1L)).isTrue();
        assertThat(tossLoginService.isLinked(2L)).isFalse();
        assertThat(tossLoginService.isLinked(3L)).isFalse();
    }

    private TossAccount createLinkedAccount(Long userId) {
        return TossAccount.builder()
                .user(createUser(userId, "ci-" + userId, "tester"))
                .tossUserKey(777L)
                .encryptedTossRefreshToken("encrypted-refresh")
                .scope("user_ci")
                .isLinked(true)
                .lastLoginAt(LocalDateTime.of(2026, 5, 14, 12, 0))
                .lastTokenRefreshedAt(LocalDateTime.of(2026, 5, 14, 12, 0))
                .build();
    }

    private TossAccount createUnlinkedAccount(Long userId) {
        return TossAccount.builder()
                .user(createUser(userId, "ci-" + userId, "tester"))
                .tossUserKey(777L)
                .encryptedTossRefreshToken(null)
                .scope("user_ci")
                .isLinked(false)
                .unlinkReferrer(TossUnlinkReferrer.UNLINK)
                .unlinkedAt(LocalDateTime.of(2026, 5, 14, 12, 30))
                .lastLoginAt(LocalDateTime.of(2026, 5, 14, 12, 0))
                .lastTokenRefreshedAt(LocalDateTime.of(2026, 5, 14, 12, 0))
                .build();
    }

    private Users createUser(Long userId, String ci, String name) {
        Users user = Users.builder()
                .ci(ci)
                .name(name)
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private String basicAuth(String rawCredentials) {
        return "Basic " + Base64.getEncoder().encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));
    }
}
