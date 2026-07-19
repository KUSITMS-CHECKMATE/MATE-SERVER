package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;

import java.time.LocalDateTime;

@Schema(description = "관리자 테스트 목록 항목")
public record AdminTestListItemResponse(
        @Schema(description = "테스트 ID", example = "1")
        Long testId,
        @Schema(description = "테스트 제목")
        String title,
        @Schema(description = "보상 금액(머니)")
        Integer reward,
        @Schema(description = "테스트 상태")
        TestStatus testStatus,
        @Schema(description = "생성 시각")
        LocalDateTime createdAt
) {
    public static AdminTestListItemResponse from(Test test) {
        return new AdminTestListItemResponse(
                test.getId(),
                test.getTitle(),
                test.getReward(),
                test.getTestStatus(),
                test.getCreatedAt()
        );
    }
}
