package server.MATE.domain.test.dto.response;

import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestCategory;
import server.MATE.domain.test.entity.TestStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record TestUpdateResponse(
        Long id,
        Long makerId,
        String title,
        String description,
        List<String> categories,
        String serviceName,
        String serviceDescription,
        List<String> imageKeys,
        TestStatus status,
        Integer goalPpl,
        Integer reward,
        LocalDateTime updatedAt
) {
    public static TestUpdateResponse from(Test test) {
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
                new ArrayList<>(test.getImageKeys()),
                test.getStatus(),
                test.getGoalPpl(),
                test.getReward(),
                test.getUpdatedAt()
        );
    }
}
