package server.MATE.domain.auth.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.auth.dto.response.TestTokenResponse;
import server.MATE.domain.auth.jwt.JwtProvider;
import server.MATE.domain.auth.jwt.TokenType;
import server.MATE.domain.auth.store.UserRefreshTokenStore;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.response.ApiResponse;

@Profile("local")
@Validated
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class TestAuthController {

    private final JwtProvider jwtProvider;
    private final UsersRepository usersRepository;
    private final UserRefreshTokenStore userRefreshTokenStore;

    @PostMapping("/local-test-token")
    public ResponseEntity<ApiResponse<TestTokenResponse>> createLocalTestToken(
            @RequestParam @NotNull Long userId,
            @RequestParam(defaultValue = "false") boolean includeRefresh
    ) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.AUTH_004));

        String accessToken = jwtProvider.generateToken(user.getId(), user.getRole().name(), TokenType.ACCESS);
        String refreshToken = null;
        if (includeRefresh) {
            refreshToken = jwtProvider.generateToken(user.getId(), user.getRole().name(), TokenType.REFRESH);
            userRefreshTokenStore.save(
                    user.getId(),
                    refreshToken,
                    jwtProvider.getExpiration(TokenType.REFRESH)
            );
        }

        TestTokenResponse response = new TestTokenResponse(
                user.getId(),
                user.getRole().name(),
                accessToken,
                refreshToken
        );

        return ResponseEntity.ok(ApiResponse.ok("로컬 테스트용 액세스 토큰을 발급했습니다.", response));
    }
}
