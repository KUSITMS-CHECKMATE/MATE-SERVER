package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.*;

import java.time.LocalDateTime;
import java.util.List;

public record TestUpdateResponse(
        Long id,
        Long makerId,
        String title,
        String description,
        List<String> categories,
        String serviceName,
        String serviceDescription,
        @Schema(description = "이미지 키·URL 쌍 목록 (key: 수정 요청용, url: 렌더링용, 30분 만료)")
        List<ImageInfo> images,
        TestStatus testStatus,
        ApprovalStatus approvalStatus,
        Integer goalPpl,
        Integer reward,
        LocalDateTime updatedAt
) {
    public static TestUpdateResponse from(Test test, List<ImageInfo> images) {
        return new TestUpdateResponse(
                test.getId(),
                test.getMakerId(),
                test.getTitle(),
                test.getDescription(),
                test.getCategories().stream()
                        .map(TestCategory::getCategory)
                        .map(Category::name)
                        .toList(),
                test.getServiceName(),
                test.getServiceDescription(),
                images,
                test.getTestStatus(),
                test.getApprovalStatus(),
                test.getGoalPpl(),
                test.getReward(),
                test.getUpdatedAt()
        );
    }
}
