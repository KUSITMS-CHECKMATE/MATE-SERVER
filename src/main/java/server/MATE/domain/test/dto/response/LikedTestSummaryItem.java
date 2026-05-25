package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.Test;

@Schema(description = "내가 찜한 테스트 목록 노출용 요약 항목")
public record LikedTestSummaryItem(
        @Schema(description = "테스트 ID", example = "1")
        Long id,
        @Schema(description = "썸네일 Public URL (만료 없음, 이미지 없으면 null)")
        String thumbnailUrl,
        @Schema(description = "테스트명", example = "남이만든테스트1")
        String title,
        @Schema(description = "테스트 한 줄 소개")
        String description,
        @Schema(description = "보상 금액(머니)", example = "600")
        Integer reward
) {
    public static LikedTestSummaryItem from(Test test, String thumbnailUrl) {
        return new LikedTestSummaryItem(
                test.getId(),
                thumbnailUrl,
                test.getTitle(),
                test.getDescription(),
                test.getReward()
        );
    }
}
