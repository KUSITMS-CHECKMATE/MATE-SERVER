package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "주관식 리포트 결과")
public record SubjectiveReportResult(
        @Schema(description = "응답 텍스트 목록 (응답 시각 오름차순, 최대 15개)")
        List<String> responses
) {
}
