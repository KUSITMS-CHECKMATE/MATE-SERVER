package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.Test;

import java.util.List;

@Schema(description = "관리자 테스트 목록 응답")
public record AdminTestListResponse(
        @Schema(description = "요청한 페이지 (1부터 시작)", example = "1")
        int page,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "전체 개수", example = "42")
        long totalCount,
        @Schema(description = "테스트 목록")
        List<AdminTestListItemResponse> tests
) {
    public static AdminTestListResponse of(int page, int size, long totalCount, List<Test> tests) {
        List<AdminTestListItemResponse> items = tests.stream()
                .map(AdminTestListItemResponse::from)
                .toList();
        return new AdminTestListResponse(page, size, totalCount, items);
    }
}
