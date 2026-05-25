package server.MATE.domain.test.dto.response;

import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestCategory;

import java.util.List;

/** 테스트 상세 */
public record TestDetailResponse(
        Long id,
        String title,
        List<String> categories,
        List<String> imageUrls,
        Integer reward,
        String description,
        String serviceName,
        String serviceDescription
) {
    public static TestDetailResponse from(Test test, List<String> imageUrls) {
        List<String> categoryCodes = test.getCategories().stream()
                .filter(testCategory -> testCategory.getDeletedAt() == null)
                .map(TestCategory::getCategory)
                .map(Category::name)
                .toList();
        return new TestDetailResponse(
                test.getId(),
                test.getTitle(),
                categoryCodes,
                imageUrls,
                test.getReward(),
                test.getDescription(),
                test.getServiceName(),
                test.getServiceDescription()
        );
    }
}
