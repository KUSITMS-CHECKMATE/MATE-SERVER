package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "카드소팅 리포트 결과")
public record CardSortingReportResult(
        @Schema(description = "그룹별 카드 배정 결과")
        List<CardSortingGroupResult> groups
) {
}
