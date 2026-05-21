package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "객관식/5초테스트(객관식) 선택지별 리포트")
public record OptionResult(
        @Schema(description = "선택지 ID", example = "101")
        Long optionId,

        @Schema(description = "선택지 텍스트", example = "검색")
        String content,

        @Schema(description = "선택 횟수", example = "8")
        int count,

        @Schema(description = "선택 비율 (%)", example = "53.3")
        double percentage
) {
}
