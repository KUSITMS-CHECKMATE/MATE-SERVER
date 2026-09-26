package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;

@Schema(description = "관리자 테스트 참여 현황")
public record AdminTestProgressResponse(
        Long testId,
        String title,
        String description,
        TestStatus testStatus,
        Integer goalPpl,
        Long pplCount,
        int achievementPercent,
        String thumbnailUrl
) {
    public static AdminTestProgressResponse from(Test test, String thumbnailUrl) {
        return new AdminTestProgressResponse(
                test.getId(),
                test.getTitle(),
                test.getDescription(),
                test.getTestStatus(),
                test.getGoalPpl(),
                test.getPplCount(),
                achievementPercent(test.getPplCount(), test.getGoalPpl()),
                thumbnailUrl
        );
    }

    // 정수 나눗셈으로 소수점 버림. 부동소수 오차로 29% → 28% 표시 방지, 목표 달성 전 100% 표시 방지
    private static int achievementPercent(Long pplCount, Integer goalPpl) {
        if (pplCount == null || goalPpl == null || goalPpl <= 0) {
            return 0;
        }
        return (int) (pplCount * 100 / goalPpl);
    }
}
