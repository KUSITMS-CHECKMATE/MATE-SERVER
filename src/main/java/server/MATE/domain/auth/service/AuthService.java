package server.MATE.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.auth.dto.response.AuthReissueResponse;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.auth.store.TossTokenStore;
import server.MATE.domain.auth.store.UserRefreshTokenStore;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final UserRefreshTokenStore userRefreshTokenStore;
    private final TossTokenStore tossTokenStore;
    private final UsersRepository usersRepository;

    public void logout(Long userId) {
        userRefreshTokenStore.delete(userId);
        tossTokenStore.deleteAccessToken(userId);
        tossTokenStore.deleteRefreshToken(userId);
    }

    public AuthReissueResponse reissue(String refreshToken) {
        jwtProvider.validateTokenTypeOrThrow(refreshToken, TokenType.REFRESH);

        Long userId = jwtProvider.extractUserId(refreshToken);
        String savedRefreshToken = userRefreshTokenStore.find(userId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.AUTH_013));

        if (!savedRefreshToken.equals(refreshToken)) throw new BaseException(BaseErrorCode.AUTH_013);

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.AUTH_004));

        String newAccessToken = jwtProvider.generateToken(userId, user.getRole().name(), TokenType.ACCESS);
        String newRefreshToken = jwtProvider.generateToken(userId, user.getRole().name(), TokenType.REFRESH);

        userRefreshTokenStore.save(userId, newRefreshToken, jwtProvider.getExpiration(TokenType.REFRESH));

        return new AuthReissueResponse(newAccessToken, newRefreshToken);
    }
}
