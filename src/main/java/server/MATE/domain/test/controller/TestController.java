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
import server.MATE.domain.test.dto.response.TestDetailResponse;
import server.MATE.domain.test.dto.response.TestLikeResponse;
import server.MATE.domain.test.dto.response.LikedTestSummaryResponse;
import server.MATE.domain.test.dto.response.MyTestSummaryResponse;
import server.MATE.domain.test.dto.response.TestSummaryResponse;
import server.MATE.domain.test.dto.response.TestUpdateResponse;
import server.MATE.domain.test.service.TestService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

import java.util.List;

@Tag(name = "[TEST] 테스트 API", description = "테스트 등록 관련 API")
@RestController
@RequestMapping("/api/v1/tests")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @Operation(
            summary = "테스트 목록 조회",
            description = """
                    전체 테스트 요약 목록을 최신순으로 조회합니다. 발견 탭 HM_01 57 화면에 해당하는 api 입니다.
                    테스트 등록 후 관리자 승인(`approvalStatus`)이 완료(`ACCEPTED`)되어야 조회됩니다.

                    - **thumbnailKey**: 업로드된 이미지 키 목록 중 첫 번째, 없으면 null을 반환
                    - **description**: 테스트 한 줄 소개
                    - **reward**: 보상 금액(머니)
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<TestSummaryResponse>>> listTests(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<TestSummaryResponse> data = testService.listTests(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("테스트 목록을 조회했습니다.", data));
    }

    @Operation(
            summary = "내가 생성한 테스트 목록 조회",
            description = """
                    현재 로그인한 사용자가 생성한 테스트 목록을 최신순으로 조회합니다.
                    승인 상태와 관계없이 삭제되지 않은 테스트를 모두 반환합니다.

                    - **testStatus**: `IN_PROGRESS`(진행 중), `COMPLETED`(완료)
                    - **title**: 테스트 제목
                    - **pplCount**: 현재 참여 인원
                    """
    )
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<MyTestSummaryResponse>>> listMyTests(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<MyTestSummaryResponse> data = testService.listMyTests(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("내가 생성한 테스트 목록을 조회했습니다.", data));
    }

    @Operation(
            summary = "내가 찜한 테스트 목록 조회",
            description = """
                    현재 로그인한 사용자가 찜한 테스트 목록을 찜한 시각 최신순으로 조회합니다.
                    삭제되지 않았고 관리자 승인(`ACCEPTED`)이 완료된 테스트만 반환합니다.

                    - **thumbnailKey**: 썸네일 이미지 키
                    - **title**: 테스트명
                    - **description**: 테스트 한 줄 소개
                    - **reward**: 보상 금액(머니)
                    """
    )
    @GetMapping("/likes")
    public ResponseEntity<ApiResponse<List<LikedTestSummaryResponse>>> listLikedTests(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<LikedTestSummaryResponse> data = testService.listLikedTests(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("내가 찜한 테스트 목록을 조회했습니다.", data));
    }

    @Operation(
            summary = "테스트 상세 조회",
            description = """
                    특정 테스트의 상세 정보를 조회합니다. TT_01 화면에 해당하는 api 입니다.
                    삭제된 테스트는 조회되지 않습니다.
                    """
    )
    @GetMapping("/{testId}")
    public ResponseEntity<ApiResponse<TestDetailResponse>> getTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestDetailResponse data = testService.getTest(testId);
        return ResponseEntity.ok(ApiResponse.ok("테스트를 조회했습니다.", data));
    }

    @Operation(summary = "테스트 찜하기", description = "테스트를 찜하고 해당 테스트의 찜 개수를 1 증가시킵니다. HM_01 화면에 해당하는 api 입니다. 이미 찜한 테스트면 현재 상태를 반환합니다.")
    @PostMapping("/{testId}/likes")
    public ResponseEntity<ApiResponse<TestLikeResponse>> likeTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestLikeResponse response = testService.likeTest(testId, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("테스트를 찜했습니다.", response));
    }

    @Operation(summary = "테스트 찜 취소", description = "테스트 찜을 취소하고 해당 테스트의 찜 개수를 1 감소시킵니다. HM_01에 해당하는 api 입니다. 찜하지 않은 테스트면 현재 상태를 반환합니다.")
    @DeleteMapping("/{testId}/likes")
    public ResponseEntity<ApiResponse<TestLikeResponse>> unlikeTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestLikeResponse response = testService.unlikeTest(testId, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("테스트 찜을 취소했습니다.", response));
    }

    @Operation(summary = "테스트 등록", description = """
            새로운 테스트를 등록합니다. MKTT_02-1 ~ MKTT_02-3 (테스트 기본 정보) 화면에 해당하는 api 입니다.
            현재 목표 인원 수(`goalPpl`), 보상 포인트(`reward`)는 Default 값으로 저장됩니다.

            카테고리
            - DAILY, FINANCE, HEALTH, SHOPPING, FOOD, GAME, CONTENT, COMMUNITY,
            AI, EDUCATION, TRAVEL, SOCIAL, CONVENIENCE, INFORMATION, BUSINESS, TRANSPORT, PUBLIC_ADMIN
            - 최소 1개 ~ 최대 3개 선택 필수

            이미지 처리
            - `imageKeys`는 이미지 업로드 URL 발급 api로 먼저 업로드한 뒤 반환된 imageKey 목록입니다.
            - 트랜잭션 실패(롤백) 시 업로드된 이미지는 Azure Blob Storage에서 자동 삭제됩니다.
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
            테스트 기본 정보를 수정합니다. MKTT_03-02 화면에 해당하는 api 입니다.

            이미지 처리
            - `imageKeys`를 전달하면 기존 이미지 목록이 전체 교체됩니다.
            - 기존 목록에서 제거된 이미지는 트랜잭션 커밋 후 S3에서 영구 삭제됩니다.
            - `imageKeys`를 `null`로 보내면 이미지는 변경되지 않습니다.
            """)
    @PatchMapping("/{testId}")
    public ResponseEntity<ApiResponse<TestUpdateResponse>> updateTest(
            @PathVariable Long testId,
            @RequestBody @Valid TestUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestUpdateResponse response = testService.updateTest(testId, request, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("테스트가 수정되었습니다.", response));
    }

    @Operation(summary = "테스트 삭제", description = "특정 테스트를 삭제(soft delete) 합니다.")
    @DeleteMapping("/{testId}")
    public ResponseEntity<ApiResponse<Void>> deleteTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        testService.deleteTest(testId, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("테스트가 삭제되었습니다.", null));
    }
}
