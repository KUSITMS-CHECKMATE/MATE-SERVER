package server.MATE.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.auth.dto.request.TossLoginRequest;
import server.MATE.domain.auth.dto.response.TossLoginResponse;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.toss.service.TossLoginService;

@Tag(name = "[AUTH] 인증 API", description = "인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AuthController {

    private final TossLoginService tossLoginService;

    @Operation(summary = "토스 로그인", description = "토스 authorization code를 사용해 Mate JWT를 발급합니다.")
    @PostMapping("/toss/login")
    public ResponseEntity<ApiResponse<TossLoginResponse>> loginWithToss(
            @RequestBody @Valid TossLoginRequest request
    ) {
        TossLoginResponse response = tossLoginService.login(request.authorizationCode(), request.referrer());
        return ResponseEntity.ok(ApiResponse.ok("토스 로그인이 완료되었습니다.", response));
    }
}
