package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestCategory;
import server.MATE.domain.test.entity.TestStatus;

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
        String serviceDescription,
        @Schema(description = "테스트 상태. `COMPLETED`(종료) 등으로 참여 버튼 비활성화에 사용")
        TestStatus testStatus,
        @Schema(description = "현재 로그인한 사용자의 테스트 응답 여부. true면 참여 버튼 비활성화")
        Boolean hasResponded
) {
    public static TestDetailResponse from(Test test, List<String> imageUrls, boolean hasResponded) {
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
                test.getServiceDescription(),
                test.getTestStatus(),
                hasResponded
        );
    }
}
