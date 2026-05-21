package server.MATE.domain.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.report.dto.response.TestReportResponse;
import server.MATE.domain.report.service.ReportService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[REPORT] 리포트 API", description = "테스트 결과 리포트 관련 API")
@RestController
@RequestMapping("/api/v1/tests/{testId}/report")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "테스트 리포트 전체 조회",
            description = """
                    메이커가 자신의 테스트에 대한 질문 목록 및 질문 유형별 응답 리포트를 조회합니다. MKST_01/MKST_02 화면에 해당합니다.
                    - 테스트 소유자(메이커)만 조회할 수 있습니다.
                    - `testStatus`가 `IN_PROGRESS`이면 `results`는 빈 리스트를 반환합니다.
                    - `testStatus`가 `COMPLETED`이면 질문 유형별 리포트가 포함됩니다.
                    - `questions`는 질문 탭(MKST_01), `results`는 결과 탭(MKST_02)에 사용됩니다.
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<TestReportResponse>> getReport(
            @PathVariable Long testId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        TestReportResponse response = reportService.getReport(testId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("리포트를 조회했습니다.", response));
    }
}
