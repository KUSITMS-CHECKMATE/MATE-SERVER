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

    @Operation(summary = "테스트 등록", description = "새로운 테스트를 등록합니다.")
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

    @Operation(summary = "테스트 수정", description = "테스트 기본 정보를 수정합니다.")
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
