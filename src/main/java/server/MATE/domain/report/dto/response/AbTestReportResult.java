package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A/B 테스트 리포트 결과")
public record AbTestReportResult(
        @Schema(description = "A안 응답 수", example = "7")
        int aCount,

        @Schema(description = "A안 응답 비율 (%)", example = "58.3")
        double aPercentage,

        @Schema(description = "B안 응답 수", example = "5")
        int bCount,

        @Schema(description = "B안 응답 비율 (%)", example = "41.7")
        double bPercentage
) {
}
