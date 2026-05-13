package server.MATE.domain.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.auth.store.TossTokenStore;
import server.MATE.domain.auth.store.UserRefreshTokenStore;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRefreshTokenStore userRefreshTokenStore;

    @Mock
    private TossTokenStore tossTokenStore;

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
}
