package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "트리테스트 노드별 리포트")
public record TreeTestNodeResult(
        @Schema(description = "노드 ID", example = "42")
        Long nodeId,

        @Schema(description = "노드 레이블", example = "설정 > 알림")
        String label,

        @Schema(description = "해당 노드를 선택한 응답 수", example = "6")
        int count,

        @Schema(description = "비율 (%)", example = "50.0")
        double percentage
) {
}
