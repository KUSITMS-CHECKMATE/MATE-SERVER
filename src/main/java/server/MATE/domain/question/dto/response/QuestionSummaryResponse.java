package server.MATE.domain.question.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.TestStatus;

import java.util.List;

@Schema(description = "질문 목록 조회 응답")
public record QuestionSummaryResponse(
        @Schema(description = "테스트 상태 (WAITING / IN_PROGRESS / COMPLETED / REJECTED)", example = "IN_PROGRESS")
        TestStatus testStatus,

        @Schema(description = "질문 개수", example = "7")
        int questionCount,

        @Schema(description = "테스트 참여자 수", example = "53")
        Long participantCount,

        @Schema(description = "질문 목록")
        List<QuestionSummaryItem> questions
) {
}
