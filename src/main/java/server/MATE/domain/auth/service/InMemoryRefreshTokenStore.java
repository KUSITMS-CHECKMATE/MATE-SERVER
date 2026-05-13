package server.MATE.domain.auth.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import server.MATE.domain.auth.jwt.TokenType;

@Component
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, StoredToken> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Long userId, String refreshToken, TokenType tokenType, long ttlMillis) {
        storage.put(key(userId, tokenType), new StoredToken(refreshToken, Instant.now().plusMillis(ttlMillis)));
    }

    private String key(Long userId, TokenType tokenType) {
        return userId + ":" + tokenType.name();
    }

    private record StoredToken(String value, Instant expiresAt) {
    }
}
