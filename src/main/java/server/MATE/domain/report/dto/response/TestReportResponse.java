package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.test.entity.TestStatus;

import java.util.List;

@Schema(description = "테스트 리포트 전체 조회 응답")
public record TestReportResponse(
        @Schema(description = "테스트 상태 (IN_PROGRESS / COMPLETED)", example = "COMPLETED")
        TestStatus testStatus,

        @Schema(description = "총 질문 수", example = "5")
        int questionCount,

        @Schema(description = "총 참여자 수", example = "12")
        Long participantCount,

        @Schema(description = "질문 목록 (순서 오름차순)")
        List<QuestionSummaryItem> questions,

        @Schema(description = "질문별 리포트 결과 (IN_PROGRESS 이면 빈 리스트)")
        List<QuestionReportItem> results
) {
}
