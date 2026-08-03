package server.MATE.domain.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.test.dto.request.RejectTestRequest;
import server.MATE.domain.test.dto.request.TestDeleteMode;
import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.dto.response.AdminTestStatusResponse;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.service.AdminTestService;
import server.MATE.domain.test.service.TestDeleteService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[ADMIN] 테스트 관리 API", description = "관리자 테스트 승인/반려 API")
@RestController
@RequestMapping("/api/v1/admin/tests")
@RequiredArgsConstructor
public class AdminTestController {

    private final AdminTestService adminTestService;
    private final TestDeleteService testDeleteService;

    @Operation(summary = "관리자 테스트 목록 조회", description = "기본값: status=WAITING, page=1, size=20, createdAt desc 정렬")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminTestListResponse>> listTests(
            @RequestParam(required = false) TestStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminTestListResponse data = adminTestService.listTests(status, page, size);
        return ResponseEntity.ok(ApiResponse.ok("관리자 테스트 목록을 조회했습니다.", data));
    }

    @Operation(summary = "관리자 테스트 상세 조회", description = "REJECTED 테스트도 조회 가능합니다.")
    @GetMapping("/{testId}")
    public ResponseEntity<ApiResponse<AdminTestDetailResponse>> getTest(@PathVariable Long testId) {
        AdminTestDetailResponse data = adminTestService.getTest(testId);
        return ResponseEntity.ok(ApiResponse.ok("테스트 상세를 조회했습니다.", data));
    }

    @Operation(summary = "테스트 승인", description = "WAITING 또는 REJECTED 상태의 테스트를 IN_PROGRESS로 전환합니다.")
    @PatchMapping("/{testId}/approve")
    public ResponseEntity<ApiResponse<AdminTestStatusResponse>> approve(@PathVariable Long testId) {
        AdminTestStatusResponse data = adminTestService.approve(testId);
        return ResponseEntity.ok(ApiResponse.ok("테스트를 승인했습니다.", data));
    }

    @Operation(summary = "테스트 반려", description = "WAITING 또는 IN_PROGRESS 상태의 테스트를 REJECTED로 전환합니다. reason은 선택값입니다.")
    @PatchMapping("/{testId}/reject")
    public ResponseEntity<ApiResponse<AdminTestStatusResponse>> reject(
            @PathVariable Long testId,
            @RequestBody(required = false) @Valid RejectTestRequest request
    ) {
        AdminTestStatusResponse data = adminTestService.reject(testId, request == null ? null : request.reason());
        return ResponseEntity.ok(ApiResponse.ok("테스트를 반려했습니다.", data));
    }

    @Operation(
            summary = "테스트 삭제",
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
