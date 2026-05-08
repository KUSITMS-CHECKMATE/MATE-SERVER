package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.Test;

import java.util.List;

@Schema(description = "테스트 목록·카드 노출용 요약")
public record TestSummaryResponse(
        @Schema(description = "테스트 ID", example = "1")
        Long id,
        /** 등록된 이미지 중 첫 번째 키. 없으면 null */
        String representativeImageKey,
        String title,
        String description,
        Integer reward,
        List<String> categories
) {
    public static TestSummaryResponse from(Test test) {
        List<String> keys = test.getImageKeys();
        String representative = keys.isEmpty() ? null : keys.getFirst();
        List<String> categories = test.getCategories().stream()
                .map(testCategory -> testCategory.getCategory().name())
                .toList();
        return new TestSummaryResponse(
                test.getId(),
                representative,
                test.getTitle(),
                test.getDescription(),
                test.getReward(),
                categories
        );
    }
}
