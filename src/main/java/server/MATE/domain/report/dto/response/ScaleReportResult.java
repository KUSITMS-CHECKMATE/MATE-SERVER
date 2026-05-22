package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "척도 리포트 결과")
public record ScaleReportResult(
        @Schema(description = "평균 점수", example = "3.8")
        double average,

        @Schema(description = "점수별 응답 분포 (1점 ~ 최대 범위)")
        List<ScaleDistributionItem> distribution
) {
}
