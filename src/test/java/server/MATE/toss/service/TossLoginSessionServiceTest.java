package server.MATE.toss.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import server.MATE.domain.auth.crypto.TokenEncryptor;
import server.MATE.domain.auth.store.TossTokenStore;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.entity.TossUnlinkReferrer;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.client.login.TossLoginApiClient;
import server.MATE.toss.dto.request.TossRefreshTokenRequest;
import server.MATE.toss.dto.response.TossTokenResponse;

@ExtendWith(MockitoExtension.class)
class TossLoginSessionServiceTest {

    @Mock
    private TossLoginApiClient tossLoginApiClient;

    @Mock
    private TossAccountRepository tossAccountRepository;

    @Mock
    private TossTokenStore tossTokenStore;

    @Mock
    private TokenEncryptor tokenEncryptor;

    @InjectMocks
    private TossLoginSessionService tossLoginSessionService;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tossLoginSessionService, "clock", fixedClock);
        ReflectionTestUtils.setField(tossLoginSessionService, "tossRefreshCacheTtlMillis", 600_000L);
    }

    @Test
    @DisplayName("Redis에 access token이 있으면 그대로 반환한다")
    void returnsCachedAccessTokenWhenPresent() {
        when(tossTokenStore.findAccessToken(1L)).thenReturn(Optional.of("cached-access"));

        Optional<String> result = tossLoginSessionService.getValidAccessToken(1L);

        assertThat(result).contains("cached-access");
        verify(tossTokenStore).findAccessToken(1L);
        verify(tossTokenStore, never()).findRefreshToken(1L);
    }

    @Test
    @DisplayName("Redis access token이 없고 Redis refresh token이 있으면 refresh 후 access token을 반환한다")
    void refreshesWithCachedRefreshTokenWhenAccessTokenMissing() {
        TossTokenResponse response = new TossTokenResponse("Bearer", "new-access", "new-refresh", 3600L, "user_ci");
        TossAccount tossAccount = createLinkedTossAccount(1L, "encrypted-old-refresh");

        when(tossTokenStore.findAccessToken(1L)).thenReturn(Optional.empty());
        when(tossTokenStore.findRefreshToken(1L)).thenReturn(Optional.of("cached-refresh"));
        when(tossLoginApiClient.refreshToken(any(TossRefreshTokenRequest.class))).thenReturn(response);
        when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.of(tossAccount));
        when(tokenEncryptor.encrypt("new-refresh")).thenReturn("encrypted-new-refresh");

        Optional<String> result = tossLoginSessionService.getValidAccessToken(1L);

        assertThat(result).contains("new-access");
        verify(tossLoginApiClient).refreshToken(new TossRefreshTokenRequest("cached-refresh"));
        verify(tossTokenStore).saveAccessToken(1L, "new-access", 3_600_000L);
        verify(tossTokenStore).saveRefreshToken(1L, "new-refresh", 600_000L);
        assertThat(tossAccount.getEncryptedTossRefreshToken()).isEqualTo("encrypted-new-refresh");
        assertThat(tossAccount.getScope()).isEqualTo("user_ci");
        assertThat(tossAccount.isLinked()).isTrue();
        assertThat(tossAccount.getUnlinkReferrer()).isNull();
        assertThat(tossAccount.getUnlinkedAt()).isNull();
        assertThat(tossAccount.getLastTokenRefreshedAt()).isEqualTo(LocalDateTime.now(fixedClock));
    }

    @Test
    @DisplayName("Redis refresh token이 없으면 DB encrypted refresh token을 복호화해 refresh 한다")
    void refreshesWithEncryptedRefreshTokenFromDatabaseWhenCacheMissing() {
        TossTokenResponse response = new TossTokenResponse("Bearer", "new-access", "new-refresh", 1800L, "user_ci,name");
        TossAccount tossAccount = createLinkedTossAccount(1L, "encrypted-db-refresh");

        when(tossTokenStore.findAccessToken(1L)).thenReturn(Optional.empty());
        when(tossTokenStore.findRefreshToken(1L)).thenReturn(Optional.empty());
        when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.of(tossAccount));
        when(tokenEncryptor.decrypt("encrypted-db-refresh")).thenReturn("db-refresh");
        when(tossLoginApiClient.refreshToken(any(TossRefreshTokenRequest.class))).thenReturn(response);
        when(tokenEncryptor.encrypt("new-refresh")).thenReturn("encrypted-new-refresh");

        Optional<String> result = tossLoginSessionService.getValidAccessToken(1L);

        assertThat(result).contains("new-access");
        verify(tokenEncryptor).decrypt("encrypted-db-refresh");
        verify(tossLoginApiClient).refreshToken(new TossRefreshTokenRequest("db-refresh"));
        verify(tossTokenStore).saveAccessToken(1L, "new-access", 1_800_000L);
        verify(tossTokenStore).saveRefreshToken(1L, "new-refresh", 600_000L);
    }

    @Test
    @DisplayName("refresh token을 찾을 수 없으면 AUTH_013 예외를 던진다")
    void throwsWhenRefreshTokenDoesNotExist() {
        when(tossTokenStore.findRefreshToken(1L)).thenReturn(Optional.empty());
        when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tossLoginSessionService.refreshLoginTokens(1L))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_013);
    }

    @Test
    @DisplayName("토큰 재발급 성공 시 Redis와 DB 상태를 갱신한다")
    void persistsTokensWhenRefreshSucceeds() {
        TossTokenResponse response = new TossTokenResponse("Bearer", "new-access", "new-refresh", 7200L, "user_ci");
        TossAccount tossAccount = createUnlinkedTossAccount(1L, "encrypted-old-refresh");

        when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.of(tossAccount));
        when(tokenEncryptor.encrypt("new-refresh")).thenReturn("encrypted-new-refresh");

        tossLoginSessionService.persistLoginTokens(1L, response);

        verify(tossTokenStore).saveAccessToken(1L, "new-access", 7_200_000L);
        verify(tossTokenStore).saveRefreshToken(1L, "new-refresh", 600_000L);
        assertThat(tossAccount.getEncryptedTossRefreshToken()).isEqualTo("encrypted-new-refresh");
        assertThat(tossAccount.getScope()).isEqualTo("user_ci");
        assertThat(tossAccount.isLinked()).isTrue();
        assertThat(tossAccount.getUnlinkReferrer()).isNull();
        assertThat(tossAccount.getUnlinkedAt()).isNull();
        assertThat(tossAccount.getLastTokenRefreshedAt()).isEqualTo(LocalDateTime.now(fixedClock));
    }

    @Test
    @DisplayName("토큰 재발급 응답에 필수값이 누락되면 AUTH_008 예외를 던진다")
    void throwsWhenTokenResponseMissingRequiredFields() {
        TossTokenResponse response = new TossTokenResponse("Bearer", null, "new-refresh", 3600L, "user_ci");

        assertThatThrownBy(() -> tossLoginSessionService.persistLoginTokens(1L, response))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_008);
    }

    @Test
    @DisplayName("연동 계정이 없으면 AUTH_004 예외를 던진다")
    void throwsWhenLinkedAccountDoesNotExist() {
        TossTokenResponse response = new TossTokenResponse("Bearer", "new-access", "new-refresh", 3600L, "user_ci");

        when(tossAccountRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tossLoginSessionService.persistLoginTokens(1L, response))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_004);
    }

    @Test
    @DisplayName("토큰 저장 중 예외가 발생하면 AUTH_009 예외로 변환한다")
    void throwsAuth009WhenTokenPersistenceFails() {
        TossTokenResponse response = new TossTokenResponse("Bearer", "new-access", "new-refresh", 3600L, "user_ci");

        doThrow(new RuntimeException("redis down"))
                .when(tossTokenStore).saveAccessToken(eq(1L), eq("new-access"), eq(3_600_000L));

        assertThatThrownBy(() -> tossLoginSessionService.persistLoginTokens(1L, response))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_009);
    }

    @Test
    @DisplayName("clearLoginTokens 호출 시 Redis access/refresh token을 모두 삭제한다")
    void clearsBothAccessAndRefreshTokens() {
        tossLoginSessionService.clearLoginTokens(1L);

        verify(tossTokenStore).deleteAccessToken(1L);
        verify(tossTokenStore).deleteRefreshToken(1L);
    }

    private TossAccount createLinkedTossAccount(Long userId, String encryptedRefreshToken) {
        return TossAccount.builder()
                .user(createUser(userId))
                .tossUserKey(777L)
                .encryptedTossRefreshToken(encryptedRefreshToken)
                .scope("user_ci")
                .isLinked(true)
                .lastLoginAt(LocalDateTime.of(2026, 5, 14, 12, 0))
                .lastTokenRefreshedAt(LocalDateTime.of(2026, 5, 14, 12, 0))
                .build();
    }

    private TossAccount createUnlinkedTossAccount(Long userId, String encryptedRefreshToken) {
        return TossAccount.builder()
                .user(createUser(userId))
                .tossUserKey(777L)
                .encryptedTossRefreshToken(encryptedRefreshToken)
                .scope("user_ci")
                .isLinked(false)
                .unlinkReferrer(TossUnlinkReferrer.UNLINK)
                .unlinkedAt(LocalDateTime.of(2026, 5, 14, 13, 0))
                .lastLoginAt(LocalDateTime.of(2026, 5, 14, 12, 0))
                .lastTokenRefreshedAt(LocalDateTime.of(2026, 5, 14, 12, 0))
                .build();
    }

    private Users createUser(Long userId) {
        Users user = Users.builder()
                .ci("ci-" + userId)
                .name("tester")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
