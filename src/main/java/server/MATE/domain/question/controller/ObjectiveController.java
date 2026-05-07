package server.MATE.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.question.dto.request.ObjectiveCreateRequest;
import server.MATE.domain.question.dto.response.ObjectiveCreateResponse;
import server.MATE.domain.question.service.ObjectiveService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[QUESTION] 문항 API", description = "문항 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/questions")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class ObjectiveController {

    private final ObjectiveService objectiveService;

    @Operation(summary = "객관식 문항 등록", description = """
            객관식 문항을 등록합니다.

            **[선택지 (options)]**
            - 최소 2개, 최대 10개까지 추가 가능합니다.
            - 선택지에 이미지를 첨부할 경우 이미지 업로드 URL 발급 API로 먼저 업로드한 뒤 받은 imageKey를 전달합니다.
            - imageKey가 없으면 null로 보내거나 필드를 생략합니다.

            **[중복 선택 (isDuplicate)]**
            - true: 여러 선택지 선택 가능. minSelect, maxSelect로 선택 개수 범위 설정 가능 (선택값)
            - false: 단일 선택만 가능. minSelect, maxSelect는 무시됩니다.

            **[기타 직접 입력 (isOther)]**
            - true: 기타(직접 입력) 선택지가 자동으로 추가됩니다.

            **[에러 코드]**
            | 코드 | HTTP | 설명 |
            |------|------|------|
            | COMMON_002 | 400 | 요청 값 검증 실패 (필수 필드 누락, 글자수 초과 등) |
            | QUESTION_001 | 400 | 최소 선택 개수는 1 이상이어야 합니다 |
            | QUESTION_002 | 400 | 최대 선택 개수는 최소 선택 개수 이상이어야 합니다 |
            | TEST_004 | 404 | 테스트를 찾을 수 없음 |
            """)
    @PostMapping("/objective")
    public ResponseEntity<ApiResponse<ObjectiveCreateResponse>> createObjective(
            @Parameter(description = "문항을 등록할 테스트 ID")
            @PathVariable Long testId,
            @RequestBody @Valid ObjectiveCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        ObjectiveCreateResponse response = objectiveService.createObjective(testId, authenticatedUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("객관식 문항이 등록되었습니다.", response));
    }
}
