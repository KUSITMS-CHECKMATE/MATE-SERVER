package server.MATE.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.MATE.domain.admin.service.AdminTestService;
import server.MATE.domain.test.dto.request.RejectTestRequest;
import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.dto.response.AdminTestStatusResponse;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.common.response.ApiResponse;

@Tag(name = "[ADMIN] 테스트 관리 API", description = "관리자 테스트 승인/반려 API")
@RestController
@RequestMapping("/api/v1/admin/tests")
@RequiredArgsConstructor
public class AdminTestController {

    private final AdminTestService adminTestService;

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
}
