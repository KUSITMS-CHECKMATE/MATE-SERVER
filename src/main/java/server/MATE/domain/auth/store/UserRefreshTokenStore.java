package server.MATE.domain.auth.store;

import java.util.Optional;

public interface UserRefreshTokenStore {

    void save(Long userId, String refreshToken, long ttlMillis);

    Optional<String> find(Long userId);

    void delete(Long userId);
}
