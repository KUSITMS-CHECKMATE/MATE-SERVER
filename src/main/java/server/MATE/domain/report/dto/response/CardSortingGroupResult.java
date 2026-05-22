package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "카드소팅 그룹별 리포트")
public record CardSortingGroupResult(
        @Schema(description = "그룹명", example = "설정")
        String groupName,

        @Schema(description = "카드별 배정 결과 (응답 수 내림차순)")
        List<CardSortingCardResult> cards
) {
}
