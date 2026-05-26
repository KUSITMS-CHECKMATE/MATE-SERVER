package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "테스트 목록 조회 응답")
public record TestSummaryListResponse(
        @Schema(description = "참여 가능한 전체 테스트 개수", example = "12")
        int testCount,
        @Schema(description = "테스트 목록")
        List<TestSummaryResponse> tests
) {
    public static TestSummaryListResponse from(List<TestSummaryResponse> tests) {
        return new TestSummaryListResponse(tests.size(), tests);
    }
}
