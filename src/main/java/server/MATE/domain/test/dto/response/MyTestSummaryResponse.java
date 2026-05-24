package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내가 생성한 테스트 목록 조회 응답")
public record MyTestSummaryResponse(
        @Schema(description = "테스트 개수", example = "3")
        int testCount,
        @Schema(description = "테스트 목록")
        List<MyTestSummaryItem> tests
) {
    public static MyTestSummaryResponse from(List<MyTestSummaryItem> tests) {
        return new MyTestSummaryResponse(tests.size(), tests);
    }
}
