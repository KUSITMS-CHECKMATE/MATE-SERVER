package server.MATE.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.auth.dto.response.TestTokenResponse;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final UsersRepository usersRepository;
    private final JwtProvider jwtProvider;

    public TestTokenResponse issueTestAccessToken(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.AUTH_004));

        String accessToken = jwtProvider.generateToken(
                user.getId(),
                user.getRole().name(),
                TokenType.ACCESS
        );

        return new TestTokenResponse(accessToken, TokenType.ACCESS.name());
    }
}
