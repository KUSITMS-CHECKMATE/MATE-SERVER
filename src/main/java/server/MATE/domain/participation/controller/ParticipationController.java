package server.MATE.domain.participation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.participation.dto.response.ParticipationCreateResponse;
import server.MATE.domain.participation.service.ParticipationService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[PARTICIPATION] 참여 API", description = "테스트 참여 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/participations")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;

    @Operation(summary = "테스트 참여 등록", description = """
            테스트에 참여하는 유저의 참여 세션을 생성합니다.
            - Request Body 없음. testId는 Path Variable, 테스터 ID는 JWT에서 추출합니다.
            """)
    @PostMapping
    public ResponseEntity<ApiResponse<ParticipationCreateResponse>> createParticipation(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        ParticipationCreateResponse response = participationService.createParticipation(testId, authenticatedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("테스트 참여가 등록되었습니다.", response));
    }
}
