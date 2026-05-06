package server.MATE.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.auth.dto.request.TestTokenRequest;
import server.MATE.domain.auth.dto.response.TestTokenResponse;
import server.MATE.domain.auth.service.AuthService;
import server.MATE.global.common.response.ApiResponse;

@Profile("local")
@Tag(name = "[AUTH] 인증 API", description = "인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "테스트용 액세스 토큰 발급", description = "토스 로그인 구현 전 사용자 ID로 테스트용 JWT를 발급합니다.")
    @PostMapping("/test-token")
    public ResponseEntity<ApiResponse<TestTokenResponse>> issueTestToken(
            @RequestBody @Valid TestTokenRequest request
    ) {
        TestTokenResponse response = authService.issueTestAccessToken(request.userId());
        return ResponseEntity.ok(ApiResponse.ok("테스트용 액세스 토큰이 발급되었습니다.", response));
    }
}
