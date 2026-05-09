package server.MATE.domain.test.dto.response;

import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestCategory;

import java.util.List;

<<<<<<< HEAD
=======
/** 테스트 상세 */
>>>>>>> origin/dev
public record TestDetailResponse(
        Long id,
        String title,
        List<String> categories,
        List<String> imageKeys,
        Integer reward,
        String description
) {
    public static TestDetailResponse from(Test test) {
        List<String> categoryCodes = test.getCategories().stream()
<<<<<<< HEAD
=======
                .filter(testCategory -> testCategory.getDeletedAt() == null)
>>>>>>> origin/dev
                .map(TestCategory::getCategory)
                .map(Category::name)
                .toList();
        List<String> keys = List.copyOf(test.getImageKeys());
        return new TestDetailResponse(
                test.getId(),
                test.getTitle(),
                categoryCodes,
                keys,
                test.getReward(),
                test.getDescription()
        );
    }
}
