package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestCategory;

import java.util.List;

/** 테스트 상세 */
public record TestDetailResponse(
        Long id,
        String title,
        List<String> categories,
        @Schema(description = "이미지 키·URL 쌍 목록 (key: 수정 요청용, url: 렌더링용, 30분 만료)")
        List<ImageInfo> images,
        Integer reward,
        String description,
        String serviceName,
        String serviceDescription
) {
    public static TestDetailResponse from(Test test, List<ImageInfo> images) {
        List<String> categoryCodes = test.getCategories().stream()
                .filter(testCategory -> testCategory.getDeletedAt() == null)
                .map(TestCategory::getCategory)
                .map(Category::name)
                .toList();
        return new TestDetailResponse(
                test.getId(),
                test.getTitle(),
                categoryCodes,
                images,
                test.getReward(),
                test.getDescription(),
                test.getServiceName(),
                test.getServiceDescription()
        );
    }
}
