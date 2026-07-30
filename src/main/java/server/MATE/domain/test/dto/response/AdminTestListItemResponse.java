package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestCategory;
import server.MATE.domain.test.entity.TestStatus;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "관리자 테스트 목록 항목")
public record AdminTestListItemResponse(
        @Schema(description = "테스트 ID", example = "1")
        Long testId,
        @Schema(description = "테스트 제목")
        String title,
        @Schema(description = "보상 금액(머니)")
        Integer reward,
        @Schema(description = "카테고리 코드 목록")
        List<String> categories,
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
                toCategoryCodes(test),
                test.getTestStatus(),
                test.getCreatedAt()
        );
    }

    private static List<String> toCategoryCodes(Test test) {
        if (test.getCategories() == null) {
            return List.of();
        }
        return test.getCategories().stream()
                .filter(testCategory -> testCategory.getDeletedAt() == null)
                .map(TestCategory::getCategory)
                .map(Category::name)
                .toList();
    }
}
