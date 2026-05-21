package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "객관식 리포트 결과")
public record ObjectiveReportResult(
        @Schema(description = "선택지별 결과 (응답 수 내림차순)")
        List<OptionResult> options
) {
}
