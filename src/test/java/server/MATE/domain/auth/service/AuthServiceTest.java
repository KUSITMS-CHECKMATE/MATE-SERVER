package server.MATE.domain.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.auth.dto.response.AuthReissueResponse;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.auth.store.TossTokenStore;
import server.MATE.domain.auth.store.UserRefreshTokenStore;
import server.MATE.domain.users.entity.Role;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRefreshTokenStore userRefreshTokenStore;

    @Mock
    private TossTokenStore tossTokenStore;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("로그아웃 시 서비스 및 토스 토큰 저장소를 모두 정리한다")
    void deletesAllTokensOnLogout() {
        authService.logout(1L);

        verify(userRefreshTokenStore).delete(1L);
        verify(tossTokenStore).deleteAccessToken(1L);
        verify(tossTokenStore).deleteRefreshToken(1L);
    }

    @Test
    @DisplayName("유효한 refresh token이면 access token과 refresh token을 재발급하고 저장소를 갱신한다")
    void reissuesTokensWhenRefreshTokenIsValid() {
        Users user = Users.builder()
                .ci("ci-1")
                .name("tester")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        when(jwtProvider.extractUserId("refresh-token")).thenReturn(1L);
        when(userRefreshTokenStore.find(1L)).thenReturn(Optional.of("refresh-token"));
        when(usersRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtProvider.generateToken(1L, "USER", TokenType.ACCESS)).thenReturn("new-access-token");
        when(jwtProvider.generateToken(1L, "USER", TokenType.REFRESH)).thenReturn("new-refresh-token");
        when(jwtProvider.getExpiration(TokenType.REFRESH)).thenReturn(1_209_600_000L);

        AuthReissueResponse response = authService.reissue("refresh-token");

        verify(jwtProvider).validateTokenTypeOrThrow("refresh-token", TokenType.REFRESH);
        verify(userRefreshTokenStore).save(1L, "new-refresh-token", 1_209_600_000L);
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("저장된 refresh token이 없으면 AUTH_013 예외를 던진다")
    void throwsWhenSavedRefreshTokenDoesNotExist() {
        when(jwtProvider.extractUserId("refresh-token")).thenReturn(1L);
        when(userRefreshTokenStore.find(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissue("refresh-token"))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_013);

        verify(jwtProvider).validateTokenTypeOrThrow("refresh-token", TokenType.REFRESH);
    }

    @Test
    @DisplayName("저장된 refresh token과 요청 토큰이 다르면 AUTH_013 예외를 던진다")
    void throwsWhenSavedRefreshTokenDoesNotMatch() {
        when(jwtProvider.extractUserId("refresh-token")).thenReturn(1L);
        when(userRefreshTokenStore.find(1L)).thenReturn(Optional.of("another-refresh-token"));

        assertThatThrownBy(() -> authService.reissue("refresh-token"))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_013);

        verify(jwtProvider).validateTokenTypeOrThrow("refresh-token", TokenType.REFRESH);
    }

    @Test
    @DisplayName("토큰의 사용자 정보가 없으면 AUTH_004 예외를 던진다")
    void throwsWhenUserDoesNotExistDuringReissue() {
        when(jwtProvider.extractUserId("refresh-token")).thenReturn(1L);
        when(userRefreshTokenStore.find(1L)).thenReturn(Optional.of("refresh-token"));
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissue("refresh-token"))
                .isInstanceOf(BaseException.class)
                .extracting(exception -> ((BaseException) exception).getErrorCode())
                .isEqualTo(BaseErrorCode.AUTH_004);

        verify(jwtProvider).validateTokenTypeOrThrow("refresh-token", TokenType.REFRESH);
    }
}
