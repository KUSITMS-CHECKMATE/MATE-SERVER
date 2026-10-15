package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "종료 테스트 목록 조회 응답")
public record CompletedTestListResponse(
        @Schema(description = "다음 페이지 존재 여부. false면 더보기 버튼 미노출 및 무한 스크롤 종료", example = "true")
        boolean hasNext,
        @Schema(description = "다음 페이지 요청 시 cursor로 그대로 전달할 값. 다음 페이지가 없으면 null")
        String nextCursor,
        @Schema(description = "종료 테스트 목록 (최근 종료 순)")
        List<TestSummaryResponse> tests
) {
}
