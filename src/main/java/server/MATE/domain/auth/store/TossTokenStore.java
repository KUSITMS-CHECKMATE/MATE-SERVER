package server.MATE.domain.auth.store;

import java.util.Optional;

public interface TossTokenStore {

    void saveAccessToken(Long userId, String accessToken, long ttlMillis);

    Optional<String> findAccessToken(Long userId);

    void deleteAccessToken(Long userId);

    void saveRefreshToken(Long userId, String refreshToken, long ttlMillis);

    Optional<String> findRefreshToken(Long userId);

    void deleteRefreshToken(Long userId);
}
