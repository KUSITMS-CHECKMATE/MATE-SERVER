package server.MATE.domain.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.test.dto.request.TestCreateRequest;
import server.MATE.domain.test.dto.request.TestUpdateRequest;
import server.MATE.domain.test.dto.response.TestCreateResponse;
import server.MATE.domain.test.dto.response.TestUpdateResponse;
import server.MATE.domain.test.service.TestService;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "[TEST] 테스트 API", description = "테스트 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @Operation(summary = "테스트 등록", description = """
            새로운 테스트를 등록합니다.

            **[이미지 처리]**
            - `imageKeys`는 S3 Presigned URL로 미리 업로드한 객체 키 목록입니다.
            - 트랜잭션 실패(롤백) 시 업로드된 이미지는 S3에서 자동 삭제됩니다.
            """)
    @PostMapping
    public ResponseEntity<ApiResponse<TestCreateResponse>> createTest(
            @RequestBody @Valid TestCreateRequest request,
            // TODO: 인증 구현 후 @AuthenticationPrincipal 등으로 대체
            @RequestHeader("X-User-Id") Long makerId
    ) {
        TestCreateResponse response = testService.createTest(request, makerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("테스트가 등록되었습니다.", response));
    }

    @Operation(summary = "테스트 수정", description = """
            테스트 기본 정보를 수정합니다.

            **[이미지 처리]**
            - `imageKeys`를 전달하면 기존 이미지 목록이 전체 교체됩니다.
            - 기존 목록에서 제거된 이미지는 트랜잭션 커밋 후 S3에서 영구 삭제됩니다.
            - `imageKeys`를 `null`로 보내면 이미지는 변경되지 않습니다.
            """)
    @PatchMapping("/{testId}")
    public ResponseEntity<ApiResponse<TestUpdateResponse>> updateTest(
            @PathVariable Long testId,
            @RequestBody @Valid TestUpdateRequest request,
            // TODO: 인증 구현 후 @AuthenticationPrincipal 등으로 대체
            @RequestHeader("X-User-Id") Long makerId
    ) {
        TestUpdateResponse response = testService.updateTest(testId, request, makerId);
        return ResponseEntity.ok(ApiResponse.ok("테스트가 수정되었습니다.", response));
    }
}
