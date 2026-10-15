package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "테스트 목록 조회 응답")
public record TestSummaryListResponse(
        @Schema(description = "참여 가능한 전체 테스트 개수 (현재 페이지가 아닌 전체 기준)", example = "12")
        long testCount,
        @Schema(description = "요청한 페이지 (1부터 시작)", example = "1")
        int page,
        @Schema(description = "페이지 크기", example = "3")
        int size,
        @Schema(description = "다음 페이지 존재 여부. false면 더보기 버튼 미노출", example = "true")
        boolean hasNext,
        @Schema(description = "테스트 목록")
        List<TestSummaryResponse> tests
) {
    public static TestSummaryListResponse of(int page, int size, long totalCount, List<TestSummaryResponse> tests) {
        boolean hasNext = (long) (page - 1) * size + tests.size() < totalCount;
        return new TestSummaryListResponse(totalCount, page, size, hasNext, tests);
    }
}
