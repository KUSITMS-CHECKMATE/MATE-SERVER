package server.MATE.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.auth.store.TossTokenStore;
import server.MATE.domain.auth.store.UserRefreshTokenStore;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRefreshTokenStore userRefreshTokenStore;
    private final TossTokenStore tossTokenStore;

    public void logout(Long userId) {
        userRefreshTokenStore.delete(userId);
        tossTokenStore.deleteAccessToken(userId);
        tossTokenStore.deleteRefreshToken(userId);
    }
}
