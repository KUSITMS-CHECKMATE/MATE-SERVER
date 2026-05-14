package server.MATE.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.auth.dto.request.AuthReissueRequest;
import server.MATE.domain.auth.dto.request.TossLoginRequest;
import server.MATE.domain.auth.dto.response.AuthReissueResponse;
import server.MATE.domain.auth.dto.response.TossLoginResponse;
import server.MATE.domain.auth.service.AuthService;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;
import server.MATE.toss.service.TossLoginService;

@Tag(name = "[AUTH] 인증 API", description = "인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ObjectProvider<TossLoginService> tossLoginServiceProvider;

    @Operation(summary = "토스 로그인", description = "토스 authorization code를 사용해 Mate JWT를 발급합니다.")
    @PostMapping("/toss/login")
    public ResponseEntity<ApiResponse<TossLoginResponse>> loginWithToss(
            @RequestBody @Valid TossLoginRequest request
    ) {
        TossLoginResponse response = getTossLoginService().login(request.authorizationCode(), request.referrer());
        return ResponseEntity.ok(ApiResponse.ok("토스 로그인이 완료되었습니다.", response));
    }

    @Operation(summary = "로그아웃", description = "현재 인증된 사용자의 Mate 로그아웃을 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        authService.logout(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("로그아웃이 완료되었습니다.", null));
    }

    @Operation(summary = "토큰 재발급", description = "MATE refresh token을 검증하고 access/refresh token을 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AuthReissueResponse>> reissue(
            @RequestBody @Valid AuthReissueRequest request
    ) {
        AuthReissueResponse response = authService.reissue(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("토큰이 재발급되었습니다.", response));
    }

    private TossLoginService getTossLoginService() {
        TossLoginService tossLoginService = tossLoginServiceProvider.getIfAvailable();
        if (tossLoginService == null) {
            throw new BaseException(BaseErrorCode.COMMON_001);
        }
        return tossLoginService;
    }
}
