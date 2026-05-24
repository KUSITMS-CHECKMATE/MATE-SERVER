package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.Test;

import java.util.List;

@Schema(description = "테스트 목록·카드 노출용 요약")
public record TestSummaryResponse(
        @Schema(description = "테스트 ID", example = "1")
        Long id,
        @Schema(description = "썸네일 Public URL (만료 없음, 이미지 없으면 null)")
        String thumbnailUrl,
        String title,
        String description,
        Integer reward,
        Long likeCount,
        Boolean isLiked,
        List<String> categories
) {
    public static TestSummaryResponse from(Test test, boolean isLiked, String thumbnailUrl) {
        List<String> categories = test.getCategories().stream()
                .filter(testCategory -> testCategory.getDeletedAt() == null)
                .map(testCategory -> testCategory.getCategory().name())
                .toList();
        return new TestSummaryResponse(
                test.getId(),
                thumbnailUrl,
                test.getTitle(),
                test.getDescription(),
                test.getReward(),
                test.getLikeCount(),
                isLiked,
                categories
        );
    }
}
