package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카드소팅 카드별 리포트")
public record CardSortingCardResult(
        @Schema(description = "순위", example = "1")
        int rank,

        @Schema(description = "카드명", example = "홈 화면")
        String cardName,

        @Schema(description = "해당 그룹에 배정한 응답 수", example = "9")
        int count,

        @Schema(description = "비율 (%)", example = "75.0")
        double percentage
) {
}
