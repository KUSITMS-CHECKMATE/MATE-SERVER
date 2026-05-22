package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.question.entity.QuestionType;

@Schema(description = "질문별 리포트 항목")
public record QuestionReportItem(
        @Schema(description = "질문 ID", example = "101")
        Long questionId,

        @Schema(description = "질문 순서", example = "1")
        Long sequence,

        @Schema(description = "질문 제목", example = "가장 자주 사용하는 기능은?")
        String title,

        @Schema(description = "질문 유형", example = "OBJECTIVE")
        QuestionType type,

        @Schema(description = "유형별 리포트 결과 (ObjectiveReportResult / SubjectiveReportResult / ScaleReportResult / AbTestReportResult / CardSortingReportResult / TreeTestReportResult)")
        Object report
) {
}
