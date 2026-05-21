package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "척도 점수별 응답 분포")
public record ScaleDistributionItem(
        @Schema(description = "점수", example = "3")
        int score,

        @Schema(description = "해당 점수 응답 수", example = "5")
        int count
) {
}
