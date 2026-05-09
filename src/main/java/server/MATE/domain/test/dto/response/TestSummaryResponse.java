package server.MATE.domain.test.dto.response;

import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestCategory;

import java.util.List;

/** 테스트 목록·카드 노출용 요약 정보 */
public record TestSummaryResponse(
        Long id,
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
                .filter(testCategory -> testCategory.getDeletedAt() == null)
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
