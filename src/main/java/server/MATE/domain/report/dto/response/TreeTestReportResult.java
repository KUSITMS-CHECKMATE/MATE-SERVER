package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "트리테스트 리포트 결과")
public record TreeTestReportResult(
        @Schema(description = "최종 선택 노드별 응답 분포 (응답 수 내림차순)")
        List<TreeTestNodeResult> nodes
) {
}
