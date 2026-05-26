package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내가 찜한 테스트 목록 조회 응답")
public record LikedTestSummaryResponse(
        @Schema(description = "테스트 개수", example = "3")
        int testCount,
        @Schema(description = "테스트 목록")
        List<LikedTestSummaryItem> tests
) {
    public static LikedTestSummaryResponse from(List<LikedTestSummaryItem> tests) {
        return new LikedTestSummaryResponse(tests.size(), tests);
    }
}
