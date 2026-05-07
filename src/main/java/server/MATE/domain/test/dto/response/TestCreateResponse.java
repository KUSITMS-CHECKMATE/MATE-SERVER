package server.MATE.domain.test.dto.response;

import server.MATE.domain.test.entity.*;

import java.time.LocalDateTime;
import java.util.List;

public record TestCreateResponse(
        Long id,
        Long makerId,
        String title,
        String description,
        List<String> categories,
        String serviceName,
        String serviceDescription,
        List<String> imageKeys,
        TestStatus testStatus,
        ApprovalStatus approvalStatus,
        Integer goalPpl,
        Integer reward,
        LocalDateTime createdAt
) {
    public static TestCreateResponse from(Test test) {
        return new TestCreateResponse(
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
                test.getImageKeys(),
                test.getTestStatus(),
                test.getApprovalStatus(),
                test.getGoalPpl(),
                test.getReward(),
                test.getCreatedAt()
        );
    }
}
