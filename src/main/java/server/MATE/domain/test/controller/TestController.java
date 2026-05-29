package server.MATE.domain.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.test.dto.request.TestDeleteMode;
import server.MATE.domain.test.dto.response.LikedTestSummaryResponse;
import server.MATE.domain.test.dto.response.MyTestSummaryResponse;
import server.MATE.domain.test.dto.response.TestDetailResponse;
import server.MATE.domain.test.dto.response.TestLikeResponse;
import server.MATE.domain.test.dto.request.TestStatusUpdateRequest;
import server.MATE.domain.test.dto.response.TestStatusUpdateResponse;
import server.MATE.domain.test.dto.response.TestSummaryListResponse;
import server.MATE.domain.test.dto.response.TestSummaryResponse;
import server.MATE.domain.test.service.TestDeleteService;
import server.MATE.domain.test.service.TestService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[TEST] 테스트 API", description = "테스트 조회/수정 관련 API")
@RestController
@RequestMapping("/api/v1/tests")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;
    private final TestDeleteService testDeleteService;

    @Operation(
            summary = "✔️ 테스트 목록 조회",
            description = """
                    전체 테스트 요약 목록을 최신순으로 조회합니다. 발견 탭 HM_01 57 화면에 해당하는 api 입니다.
                    추후 페이지네이션 적용하여 무한 스크롤 지원하도록 리팩토링이 필요합니다.

                    - **testCount**: 참여 가능한 전체 테스트 개수
                    - **thumbnailUrl**: 업로드된 이미지 중 첫 번째의 Public URL, 없으면 null을 반환
                    - **description**: 테스트 한 줄 소개
                    - **reward**: 보상 금액(머니)
                    - ui상 사용하지 않는 필드: likeCount, categories
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<TestSummaryListResponse>> listTests(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestSummaryListResponse data = testService.listTests(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("테스트 목록을 조회했습니다.", data));
    }

    @Operation(
            summary = "️✔️ 내 테스트 목록 조회",
            description = """
                    현재 로그인한 사용자가 생성한 테스트 목록을 최신순으로 조회합니다. 테스트 탭 MKTT_01 화면에 해당하는 api 입니다.<br>
                    삭제되지 않은 테스트를 모두 반환합니다.
                    
                    - **testCount**: 테스트 개수
                    - **testStatus**: `WAITING`(검수 중), `IN_PROGRESS`(진행 중), `COMPLETED`(종료), `REJECTED`(반려)
                    - **title**: 테스트 제목
                    - **pplCount**: 현재 참여 인원
                    - **goalPpl**: 테스트 가능 최대 인원수
                    """
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyTestSummaryResponse>> listMyTests(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        MyTestSummaryResponse data = testService.listMyTests(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("내 테스트 목록을 조회했습니다.", data));
    }

    @Operation(
            summary = "✔️ 찜한 테스트 목록 조회",
            description = """
                    현재 로그인한 사용자가 찜한 테스트 목록을 찜한 시각 최신순으로 조회합니다. 관심 탭 HM_01 19 화면에 해당하는 api 입니다.<br>
                    추후 페이지네이션 적용하여 무한 스크롤 지원하도록 리팩토링이 필요합니다.
                    
                    - **testCount**: 테스트 개수
                    - **id**: 테스트 ID
                    - **thumbnailUrl**: 썸네일 Public URL (만료 없음, 이미지 없으면 null)
                    - **title**: 테스트명
                    - **description**: 테스트 한 줄 소개
                    - **reward**: 보상 금액(머니)
                    """
    )
    @GetMapping("/likes")
    public ResponseEntity<ApiResponse<LikedTestSummaryResponse>> listLikedTests(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        LikedTestSummaryResponse data = testService.listLikedTests(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("내가 찜한 테스트 목록을 조회했습니다.", data));
    }

    @Operation(
            summary = "✔️ 테스트 상세 조회",
            description = """
                    특정 테스트의 상세 정보를 조회합니다. TT_01 화면에 해당하는 api 입니다.
                    삭제된 테스트는 조회되지 않습니다.<br>
                    
                    - **testStatus**: `WAITING`(검수 중), `IN_PROGRESS`(진행 중), `COMPLETED`(종료), `REJECTED`(반려). 종료(`COMPLETED`) 시 참여 버튼 비활성화
                    - **hasResponded**: 현재 로그인한 사용자가 이미 응답했으면 true. true면 참여 버튼 비활성화
                    """
    )
    @GetMapping("/{testId}")
    public ResponseEntity<ApiResponse<TestDetailResponse>> getTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestDetailResponse data = testService.getTest(testId, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("테스트를 조회했습니다.", data));
    }

    @Operation(
            summary = "✔️ 테스트 상태 변경",
            description = """
                    테스트 상태를 변경합니다.

                    - `COMPLETED`: 메이커 본인만 가능, 현재 상태가 `IN_PROGRESS`일 때만 허용
                    - `IN_PROGRESS`: 관리자만 가능 (테스트 승인)
                    - `REJECTED`: 관리자만 가능 (테스트 반려)
                    """
    )
    @PatchMapping("/{testId}/status")
    public ResponseEntity<ApiResponse<TestStatusUpdateResponse>> updateTestStatus(
            @PathVariable Long testId,
            @RequestBody @Valid TestStatusUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestStatusUpdateResponse response = testService.updateTestStatus(
                testId, authenticatedUser.getId(), authenticatedUser.getRole(), request.status());
        return ResponseEntity.ok(ApiResponse.ok("테스트 상태가 변경되었습니다.", response));
    }

    @Operation(summary = "✔️ 테스트 찜하기",
            description = "테스트를 찜하고 해당 테스트의 찜 개수를 1 증가시킵니다. HM_01 화면에 해당하는 api 입니다. 이미 찜한 테스트면 현재 상태를 반환합니다.")
    @PostMapping("/{testId}/likes")
    public ResponseEntity<ApiResponse<TestLikeResponse>> likeTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestLikeResponse response = testService.likeTest(testId, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("테스트를 찜했습니다.", response));
    }

    @Operation(summary = "✔️ 테스트 찜 취소",
            description = "테스트 찜을 취소하고 해당 테스트의 찜 개수를 1 감소시킵니다. HM_01에 해당하는 api 입니다. 찜하지 않은 테스트면 현재 상태를 반환합니다.")
    @DeleteMapping("/{testId}/likes")
    public ResponseEntity<ApiResponse<TestLikeResponse>> unlikeTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestLikeResponse response = testService.unlikeTest(testId, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("테스트 찜을 취소했습니다.", response));
    }

    @Operation(
            summary = "🔐️ 테스트 삭제",
            description = """
                    테스트를 soft 삭제 또는 hard 삭제합니다. 해당 api는 관리자 계정으로만 요청 가능합니다.
                    - `mode=SOFT`: 테스트와 연관된 엔티티를 논리 삭제합니다.
                      - soft delete 대상: `test`, `question`, `answer`, `participation`, `report`
                      - 유지 대상: `test_like`, `payment`, `promotion_reward`, `test_category`
                    
                    - `mode=HARD`: 테스트와 연관된 엔티티를 모두 영구 삭제합니다. **요청 시, X-MATE-Hard-Delete-Key 헤더에 비밀키를 입력해주세요.**
                      - hard delete 대상: `test`, `question`, `answer`, `participation`, `report`,
                          `test_like`, `test_category`, `payment`, `promotion_reward`,
                          `objective`, `objective_option`, `subjective`, `ab_test`, `scale`, `card_sorting`,
                          `five_second`, `five_second_option`, `tree_test`
                      - 또한, 테스트/질문에 사용된 이미지 파일도 blob storage에서 함께 영구 삭제됩니다.
                    """

    )
    @DeleteMapping("/{testId}")
    public ResponseEntity<ApiResponse<Void>> deleteTest(
            @PathVariable Long testId,
            @RequestParam TestDeleteMode mode,
            @Parameter(description = "하드 삭제 검증 키. mode=HARD 일 때 필수입니다.")
            @RequestHeader(value = "X-MATE-Hard-Delete-Key", required = false) String hardDeleteKey,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        testDeleteService.deleteTest(
                testId,
                authenticatedUser.getRole(),
                mode,
                hardDeleteKey
        );
        return ResponseEntity.ok(ApiResponse.ok("테스트를 삭제했습니다.", null));
    }
}
