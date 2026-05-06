package server.MATE.domain.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.test.dto.request.TestCreateRequest;
import server.MATE.domain.test.dto.request.TestUpdateRequest;
import server.MATE.domain.test.dto.response.TestCreateResponse;
import server.MATE.domain.test.dto.response.TestSummaryResponse;
import server.MATE.domain.test.dto.response.TestUpdateResponse;
import server.MATE.domain.test.service.TestService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

import java.util.List;

@Tag(name = "[TEST] 테스트 API", description = "테스트 목록 조회·등록·수정·삭제 API")
@RestController
@RequestMapping("/api/v1/tests")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @Operation(
            summary = "테스트 목록 조회",
            description = """
                    전체 테스트 요약 목록을 조회합니다. (삭제된 항목 제외)

                    - **representativeImageKey**: 업로드된 이미지 키 목록 중 첫 번째. 없으면 null입니다.
                    - **description**: 테스트 한 줄 소개
                    - **reward**: 보상 금액(머니)
                    목록은 생성일 기준 최신순입니다.
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<TestSummaryResponse>>> listTests() {
        List<TestSummaryResponse> data = testService.listTests();
        return ResponseEntity.ok(ApiResponse.ok("테스트 목록을 조회했습니다.", data));
    }

    @Operation(summary = "테스트 등록", description = """
            새로운 테스트를 등록합니다.

            **[categories]**
            DAILY, FINANCE, HEALTH, SHOPPING, FOOD, GAME, CONTENT, COMMUNITY,
            AI, EDUCATION, TRAVEL, SOCIAL, CONVENIENCE, INFORMATION, BUSINESS, TRANSPORT, PUBLIC_ADMIN
            (1~3개 선택 필수)

            **[이미지 처리]**
            - imageKeys는 이미지 업로드 URL 발급 API로 먼저 업로드한 뒤 받은 imageKey 목록입니다.
            - 트랜잭션 실패(롤백) 시 업로드된 이미지는 Azure Blob Storage에서 자동 삭제됩니다.

            **[에러 코드]**
            | 코드 | HTTP | 설명 |
            |------|------|------|
            | COMMON_002 | 400 | 요청 값 검증 실패 (필수 필드 누락, 글자수 초과 등) |
            | TEST_001 | 400 | 카테고리는 1~3개 선택 필수 |
            | TEST_002 | 400 | 이미지는 최대 10개까지 업로드 가능 |
            """)
    @PostMapping
    public ResponseEntity<ApiResponse<TestCreateResponse>> createTest(
            @RequestBody @Valid TestCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestCreateResponse response = testService.createTest(request, authenticatedUser.getId());
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

    @Operation(summary = "테스트 삭제", description = "테스트를 삭제합니다.")
    @DeleteMapping("/{testId}")
    public ResponseEntity<ApiResponse<Void>> deleteTest(
            @PathVariable Long testId,
            // TODO: 인증 구현 후 @AuthenticationPrincipal 등으로 대체
            @RequestHeader("X-User-Id") Long makerId
    ) {
        testService.deleteTest(testId, makerId);
        return ResponseEntity.ok(ApiResponse.ok("테스트가 삭제되었습니다.", null));
    }
    
}
