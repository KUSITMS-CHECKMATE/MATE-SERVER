package server.MATE.domain.question.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.question.entity.QuestionType;

@Schema(description = "질문 목록 항목")
public record QuestionSummaryItem(
        @Schema(description = "질문 고유번호", example = "101")
        Long questionId,

        @Schema(description = "질문 순서", example = "1")
        Long sequence,

        @Schema(description = "질문 제목", example = "가장 자주 사용하는 기능은 무엇인가요?")
        String title,

        @Schema(description = "질문 유형", example = "OBJECTIVE")
        QuestionType type
) {
}
