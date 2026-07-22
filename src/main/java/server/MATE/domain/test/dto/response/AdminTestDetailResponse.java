package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestCategory;
import server.MATE.domain.test.entity.TestStatus;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "관리자 테스트 상세")
public record AdminTestDetailResponse(
        Long testId,
        String title,
        String description,
        Integer reward,
        List<String> categories,
        List<String> imageUrls,
        TestStatus testStatus,
        String rejectionReason,
        LocalDateTime createdAt
) {
    public static AdminTestDetailResponse from(Test test, List<String> imageUrls) {
        List<String> categoryCodes = test.getCategories().stream()
                .filter(testCategory -> testCategory.getDeletedAt() == null)
                .map(TestCategory::getCategory)
                .map(Category::name)
                .toList();
        return new AdminTestDetailResponse(
                test.getId(),
                test.getTitle(),
                test.getDescription(),
                test.getReward(),
                categoryCodes,
                imageUrls,
                test.getTestStatus(),
                test.getRejectionReason(),
                test.getCreatedAt()
        );
    }
}
