package server.MATE.domain.auth.service;

import server.MATE.domain.auth.jwt.TokenType;

public interface RefreshTokenStore {

    void save(Long userId, String refreshToken, TokenType tokenType, long ttlMillis);
}
