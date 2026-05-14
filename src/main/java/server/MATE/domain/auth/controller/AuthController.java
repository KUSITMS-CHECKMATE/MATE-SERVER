package server.MATE.domain.auth.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import server.MATE.domain.auth.dto.request.AuthReissueRequest;
import server.MATE.domain.auth.dto.request.TossLoginRequest;
import server.MATE.domain.auth.dto.response.AuthReissueResponse;
import server.MATE.domain.auth.dto.response.TossLinkStatusResponse;
import server.MATE.domain.auth.dto.response.TossLoginResponse;
import server.MATE.domain.auth.service.AuthService;
import server.MATE.domain.users.entity.Role;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;
import server.MATE.domain.users.entity.TossUnlinkReferrer;
import server.MATE.toss.dto.request.TossUnlinkByUserKeyRequest;
import server.MATE.toss.dto.request.TossUnlinkCallbackRequest;
import server.MATE.toss.service.TossLoginService;

@Tag(name = "[AUTH] 인증 API", description = "인증 관련 API")
@Validated
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

    @Operation(summary = "로그아웃", description = "현재 인증된 사용자의 Mate 로그아웃 합니다. 별도 요청값 없이 헤더에 포함된 access 토큰으로 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        authService.logout(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("로그아웃이 완료되었습니다.", null));
    }

    @Operation(summary = "토큰 재발급", description = "refresh 토큰으로 요청하면 새 access 토큰과 refresh 토큰을 발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AuthReissueResponse>> reissue(
            @RequestBody @Valid AuthReissueRequest request
    ) {
        AuthReissueResponse response = authService.reissue(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("토큰이 재발급되었습니다.", response));
    }

    @Operation(
            summary = "토스 연결 해제",
            description = "현재 사용자의 토스 로그인 연결을 해제합니다. 토스 앱에서 연결이 해제되면 서비스 세션도 종료되어 재로그인이 필요합니다."
    )
    @PostMapping("/toss/unlink")
    public ResponseEntity<ApiResponse<Void>> unlinkToss(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        getTossLoginService().unlinkCurrentUser(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("토스 연결이 해제되었습니다.", null));
    }

    @Operation(
            summary = "토스 연결 해제(userKey)",
            description = "운영/관리/보정용 API입니다. 프론트에서 직접 호출하는 API가 아닙니다."
    )
    @PostMapping("/toss/unlink/by-user-key")
    public ResponseEntity<ApiResponse<Void>> unlinkTossByUserKey(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody @Valid TossUnlinkByUserKeyRequest request
    ) {
        validateAdminRole(authenticatedUser);
        getTossLoginService().unlinkByUserKey(request.userKey(), TossUnlinkReferrer.UNLINK);
        return ResponseEntity.ok(ApiResponse.ok("토스 연결이 해제되었습니다.", null));
    }

    @Operation(summary = "토스 연동 상태 조회", description = "현재 사용자의 토스 로그인 연동 상태를 조회합니다.")
    @GetMapping("/toss/integration-status")
    public ResponseEntity<ApiResponse<TossLinkStatusResponse>> getTossIntegrationStatus(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TossLoginService tossLoginService = tossLoginServiceProvider.getIfAvailable();
        boolean isLinked = tossLoginService != null && tossLoginService.isLinked(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(
                "토스 연동 상태를 조회했습니다.",
                new TossLinkStatusResponse(isLinked)
        ));
    }

    // @Hidden
    @Operation(
            summary = "토스 연결 해제 콜백",
            description = "토스 시스템이 호출하는 운영용 콜백 API입니다. 프론트에서 직접 호출하는 API가 아닙니다. Basic Auth 헤더 검증이 필요합니다."
    )
    @GetMapping("/toss/login/unlink/callback")
    public ResponseEntity<ApiResponse<Void>> handleTossUnlinkCallbackGet(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam @NotNull Long userKey,
            @RequestParam @NotBlank String referrer
    ) {
        TossLoginService tossLoginService = getTossLoginService();
        tossLoginService.validateCallbackAuthorization(authorization);
        tossLoginService.handleUnlinkCallback(userKey, TossUnlinkReferrer.from(referrer));
        return ResponseEntity.ok(ApiResponse.ok("토스 연결 해제 콜백을 처리했습니다.", null));
    }

    // @Hidden
    @Operation(
            summary = "토스 연결 해제 콜백",
            description = "토스 시스템이 호출하는 운영용 콜백 API입니다. 프론트에서 직접 호출하는 API가 아닙니다. Basic Auth 헤더 검증이 필요합니다."
    )
    @PostMapping("/toss/login/unlink/callback")
    public ResponseEntity<ApiResponse<Void>> handleTossUnlinkCallbackPost(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody @Valid TossUnlinkCallbackRequest request
    ) {
        TossLoginService tossLoginService = getTossLoginService();
        tossLoginService.validateCallbackAuthorization(authorization);
        tossLoginService.handleUnlinkCallback(request.userKey(), TossUnlinkReferrer.from(request.referrer()));
        return ResponseEntity.ok(ApiResponse.ok("토스 연결 해제 콜백을 처리했습니다.", null));
    }

    private TossLoginService getTossLoginService() {
        TossLoginService tossLoginService = tossLoginServiceProvider.getIfAvailable();
        if (tossLoginService == null) {
            throw new BaseException(BaseErrorCode.COMMON_001);
        }
        return tossLoginService;
    }

    private void validateAdminRole(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getRole() != Role.ADMIN) {
            throw new BaseException(BaseErrorCode.COMMON_009);
        }
    }
}
